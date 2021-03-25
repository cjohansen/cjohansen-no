--------------------------------------------------------------------------------
:type :meta
:title Optimized Optimus Asset Paths from ClojureScript
:published #time/ldt "2018-03-31T12:00"
:updated #time/ldt "2021-03-25T10:00"
:tags [:clojure]
--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title Optimized Optimus Asset Paths from ClojureScript
:body

If you're building a web application with Clojure, chances are you either are or
should be using [Optimus](https://github.com/magnars/optimus) to optimize and
serve your assets. One of the optimizations Optimus can employ is to create
"cache buster" URLs, which means that `/images/logo.png` in development might
become `/images/0951812be272/logo.png` in production. Optimus provides
server-side resolution of URLs like this through its
[optimus.link](https://github.com/magnars/optimus#using-the-new-urls) namespace.
This post explains how to refer to optimized URLs from ClojureScript.

--------------------------------------------------------------------------------
:type :section
:title Macros to the rescue
:body

ClojureScript macros run in Clojure, and provide the necessary link between the
two worlds. To access Optimus asset paths in ClojureScript, we will load some
assets, provide a function that finds the preferred path to an asset, and
finally expose it via a ClojureScript macro.

In a Clojure namespace (e.g. in a file with a clj extension), load some assets
with Optimus:

```clj
(ns myapp.assets
  (:require [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]))

(def assets (-> (assets/load-assets "public" [#"/images/.*\..+$"])
                optimizations/all))
```

Next up, we will add a function to find an asset's _preferred path_ - that is,
if the asset is available under a new path, use that, otherwise use the original
path:

```clj
(defn preferred-path [path]
  (->> assets
       (filter #(= path (assets/original-path %)))
       first
       :path))
```

Finally, provide a macro that calls the `preferred-path` function:

```clj
(defmacro asset-path [path]
  (preferred-path path))
```

From a ClojureScript namespace, require and use the macro (make sure that
optimus is on the classpath of the process compiling ClojureScript - e.g. your
build process, your figwheel server, etc):

```clj
(ns myapp.core
  (:require-macros [myapp.assets :refer [asset-path]]))

(asset-path "/images/logo.png") ;;=> /images/0951812be272/logo.png
```

Voila! That's pretty much it. Apart from this, there are some gotchas you need
to account for when wiring your app together.

## Resolving Dynamic Paths

Because ClojureScript macros are resolved compile-time, this only works with
inline paths as shown above. This will not work:

```clj
(ns myapp.core
  (:require-macros [myapp.assets :refer [asset-path]]))

(let [path "/images/logo.png"]
  (asset-path path))
```

This will literally look up the symbol `path`, which will not match anything,
and then `path` will be evaluated runtime and resolved to `"/images/logo.png"`.

There is no way to fix this, but we can build the macro a little bit differently
and make it work:

```clj
(defmacro get-asset-paths []
  (->> assets
       (filter :original-path)
       (map (juxt :original-path :path))
       (into {})))
```

This macro will return a map of asset paths to optimized asset paths (these will
be the same when there are no optimizations). You can then provide a
ClojureScript function to look it up. Create `assets.cljs` like so:

```clj
(ns ui.assets
  (:require-macros [ui.assets :refer [get-asset-paths]]))

(def asset-paths (get-asset-paths))

(defn get-asset-path [asset]
  (get asset-paths asset asset))
```

This `get-asset-path` function can be used just like any other function, with
dynamic paths, and what have you. The trade-off is that your build will contain
a list of all the assets you expect to look up, so you want to use this with
care if you have lots of assets.

## Avoid Circular Dependencies

If you use Optimus to cache-bust your built ClojureScript bundle, you must
exempt it from the assets to refer from ClojureScript, or your project will fail
to build. Personally, I only need to refer to images from ClojureScript, so I
solved this problem by splitting bundles and other assets:

```clj
(def assets (assets/load-assets "public" [#"/images/.*\..+$"]))
(def bundles (assets/load-bundles "public" {"styles.css" ["/styles/main.css"]
                                            "app.js" ["/js/app.js"]}))
(def all-assets (concat bundles assets))
```

The `preferred-path` function (and thus, the CLJS macro) will only look up
`assets`, while the server middleware will use `all-assets` to optimize and
serve all assets.

## Resolve Asset Optimizations Compile-Time

Because macros run compile-time, data that will be exposed must be available
compile-time. This means that Optimus assets, and what kind of optimizations to
apply to them must be determined with compile-time, not with runtime
configuration. Configuration loaded with
[component](https://github.com/stuartsierra/component),
[mount](https://github.com/tolitius/mount), and similar tools cannot be used to
determine what optimizations to enable.

There are many ways to resolve optimizations compile-time. Environment variables
and system properties are two obvious choices. I will detail a solution that
defaults to the production settings, while allowing overrides via system
properties, which can be set via e.g. Leiningen profiles or tools.deps JVM
options.

### Asset Config

We will need a function that can read a system property as a boolean, and fall
back to a default if the property is not set:

```clj
(defn bool-property [prop default]
  (if-let [property (System/getProperty prop)]
    (Boolean/parseBoolean property)
    default))
```

We will use it to load the asset configuration:

```clj
(def asset-config
  {:live? (bool-property "optimus.assets.live" false)
   :optimize? (bool-property "optimus.assets.optimize" true)})
```

Then we define funtions to load assets:

```clj
(defn get-assets []
  (assets/load-assets "public" [#"/images/.*\..+$"]))

(defn get-bundles []
  (assets/load-bundles "public" {"styles.css" ["/styles/main.css"]
                                 "app.js" ["/js/app.js"]}))

(defn get-all-assets []
  (concat
   (get-bundles)
   (get-assets)))

(defn optimize-assets [assets & [options]]
  (if (:optimize? asset-config)
    (optimizations/all assets options)
    (optimizations/none assets options)))
```

We can use these building blocks to create a Ring middleware to load assets:

```clj
(defn wrap-assets [handler]
  (optimus/wrap
   handler
   get-all-assets
   optimize-assets
   (if (:live? asset-config)
     strategies/serve-live-assets
     strategies/serve-frozen-assets)))
```

And finally, we can use the same building-blocks to load assets and provide the
ClojureScript macro to resolve paths:

```clj
(def assets (optimize-assets (get-assets)))

(defmacro get-asset-paths []
  (->> assets
       (map (juxt :original-path :path))
       (into {})))
```

In `project.clj`, you can set system properties in the dev profile to skip
costly optimizations during development:

```clj
(defproject myapp "0.1.0-SNAPSHOT"
  ;; ...
  :profiles {:dev {;;...
                   :jvm-opts ["-Doptimus.assets.optimize=false"
                              "-Doptimus.assets.live=true"]}})
```

Beware that the dev profile is loaded by default, so you'll want to provide a
profile to use for building your production bundles:

```clj
(defproject myapp "0.1.0-SNAPSHOT"
  ;; ...
  :profiles {:dev {;;...
                   :jvm-opts ["-Doptimus.assets.optimize=false"
                              "-Doptimus.assets.live=true"]}
             :prod {;;...
                   :jvm-opts ["-Doptimus.assets.optimize=true"
                              "-Doptimus.assets.live=false"]}})
```

And use that for building:

```sh
lein with-profile prod cljsbuild once min
```

If you use tools.deps, you can set properties like so:

```sh
clojure -J'-Doptimus.assets.optimize=false' -A:dev
```

## A Note on Compile-time vs Runtime

One drawback with this solution is that optimization settings is a compile-time
decision for ClojureScript, but a runtime decision for Clojure. This means that
you need to take care to run the server with the runtime configuration that
corresponds to how you compiled your ClojureScript. Failing to do so may cause
the frontend to refer to assets that the server does not know about. Shouldn't
be a huge problem, but it is a potential pitfall.

## Full Listing

For reference, here is the full `src/myapp/assets.clj`:

```clj
(ns myapp.assets
  (:require [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :as strategies]))

(defn bool-property [prop default]
  (if-let [property (System/getProperty prop)]
    (Boolean/parseBoolean property)
    default))

(def asset-config
  {:live? (bool-property "optimus.assets.live" false)
   :optimize? (bool-property "optimus.assets.optimize" true)})

(defn get-assets []
  (assets/load-assets "public" [#"/images/.*\..+$"]))

(defn get-bundles []
  (assets/load-bundles "public" {"styles.css" ["/styles/main.css"]
                                 "app.js" ["/js/app.js"]}))

(defn get-all-assets []
  (concat
   (get-bundles)
   (get-assets)))

(defn optimize-assets [assets & [options]]
  (if (:optimize? asset-config)
    (optimizations/all assets options)
    (optimizations/none assets options)))

(defn wrap-assets
  [handler]
  (optimus/wrap
   handler
   get-all-assets
   optimize-assets
   (if (:live? asset-config)
     strategies/serve-live-assets
     strategies/serve-frozen-assets)))

(def assets (optimize-assets (get-assets)))

(defmacro get-asset-paths []
  (->> assets
       (map (juxt :original-path :path))
       (into {})))
```

And the full `src/myapp/assets.cljs`:

```clj
(ns ui.assets
  (:require-macros [ui.assets :refer [get-asset-paths]]))

(def asset-paths (get-asset-paths))

(defn get-asset-path [asset]
  (get asset-paths asset asset))
```

## Acknowledgements

Thanks to the ever-awesome [Magnar Sveen](https://github.com/magnars) for
hashing out the idea for this with me.
