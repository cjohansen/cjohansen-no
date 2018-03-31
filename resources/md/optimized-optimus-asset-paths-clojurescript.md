# Optimized Optimus Asset Paths from ClojureScript

If you're building a web application with Clojure, chances are you either are or
should be using [Optimus](https://github.com/magnars/optimus) to optimize and
serve your assets. One of the optimizations Optimus can employ is to create
"cache buster" URLs, which means that `/images/logo.png` in development might
become `/images/0951812be272/logo.png` in production. Optimus provides
server-side resolution of URLs through its
[optimus.link](https://github.com/magnars/optimus#using-the-new-urls) namespace.
This post explains how to refer to optimized URLs from ClojureScript.

## Macros to the rescue

ClojureScript macros run in Clojure, and provide the necessary link between the
two worlds. To access Optimus asset paths in ClojureScript, we will load some
assets, provide a function that finds the preferred path to an asset, and
finally expose it via a ClojureScript macro.

In a Clojure namespace, load some assets with Optimus:

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
  (or (->> assets
           (filter #(= path (:original-path %)))
           first
           :path)
      path))
```

Finally, provide a macro that calls the `preferred-path` function:

```clj
(defmacro asset-path [path]
  (preferred-path path))
```

From a ClojureScript namespace, require and use the macro:

```clj
(ns myapp.core
  (:require-macros [myapp.assets :refer [asset-path]]))

(asset-path "/images/logo.png") ;;=> /images/0951812be272/logo.png
```

Voila! That's pretty much it. Apart from this, there are two gotchas you need to
account for when wiring your app together.

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
properties, which can be set via e.g. Leiningen profiles.

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

(defn preferred-path [path]
  (or (->> assets
           (filter #(= path (:original-path %)))
           first
           :path)
      path))

(defmacro asset-path [path]
  (preferred-path path))
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

### A Note on Compile-time vs Runtime

One drawback with this solution is that optimization settings is a compile-time
decision for ClojureScript, but a runtime decision for Clojure. This means that
you need to take care to run the server with the runtime configuration that
corresponds to how you compiled your ClojureScript. Failing to do so may cause
the frontend to refer to assets that the server does not know about. Shouldn't
be a huge problem, but it is a potential pitfall.

## Full Listing

For reference, here is the full assets namespace:

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

(defn preferred-path [path]
  (or (->> assets
           (filter #(= path (:original-path %)))
           first
           :path)
      path))

(defmacro asset-path [path]
  (preferred-path path))
```
