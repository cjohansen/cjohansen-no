# tools.deps, figwheel-main, Devcards, and Emacs

Thanks to [tools.deps](https://github.com/clojure/tools.deps.alpha), setting up
a ClojureScript project in 2019 can be simpler (in terms of moving pieces) than
it used to be, but there are still pitfalls.

All these tools have excellent reference docs, but sometimes I miss
cross-cutting tutorial-style docs on how to piece everything together. So
without any further ado, here is a no-frills guide to setting up a tools.deps
project with [figwheel-main](https://figwheel.org) and
[Devcards](https://github.com/bhauman/devcards) that can be used with
Emacs/CIDER or from a shell. Towards the end you'll find bonus sections on
running tests and deploying your ClojureScript project as a
[Clojars](http://clojars.org/) module for others to use.

## 1. Install tools.deps

The official docs already have very good [getting started
instructions](https://clojure.org/guides/getting_started), refer to them for
details. On OSX:

```sh
brew install clojure
```

## 2. Create a deps.edn file

You will need some cljs/cljc sources in `src/my_app_ns/`. Create a file in the
root of your project called `deps.edn`, and fill it with:

```clj
{:paths ["src"]
 :deps {org.clojure/clojure {:mvn/version "1.10.0"}
        org.clojure/clojurescript {:mvn/version "1.10.439"}}
 :aliases {:dev {:extra-paths ["resources"]
                 :extra-deps {com.bhauman/figwheel-main {:mvn/version "0.2.0-SNAPSHOT"}}}}}
```

## 3. Add dev.cljs.edn

Figwheel uses `dev.cljs.edn` to configure the ClojureScript compiler, and
optionally, figwheel itself as well. Here's a minimal version:

```clj
{:main my-app-ns.core
 :optimizations :none
 :pretty-print true
 :source-map true
 :asset-path "/js/dev"
 :output-to "resources/public/js/dev.js"
 :output-dir "resources/public/js/dev"}
```

Now you can launch a figwheel server from a shell:

```sh
clojure -A:dev -m figwheel.main -b dev -r
```

This will pop up a browser window and drop you into a shell. **NB!** While you
can add the command line arguments to `:main-opts` I strongly suggest you
don't until you've read through [the Emacs section](#emacs).

## 4. Add Devcards

figwheel-main supports multiple mains from the same build, which means that you
can serve both your development build and devcards from the same figwheel
process. It's amazing. In the root of your project, create the
`devcards/my_app_ns` directory, and add the following to
`devcards/my_app_ns/cards.cljs`:

```clj
(ns ^:figwheel-hooks my-app-ns.cards
  (:require [devcards.core]
            [my-app-ns.cards.my-first-devcard]))

(enable-console-print!)

(defn render []
  (devcards.core/start-devcard-ui!))

(defn ^:after-load render-on-relaod []
  (render))

(render)
```

You will need to require all your devcards into this namespace.

Next, add the Devcards dependency and the extra directory to your `deps.edn`
file:

```clj
{:paths ["src"]
 :deps {org.clojure/clojure {:mvn/version "1.10.0"}
        org.clojure/clojurescript {:mvn/version "1.10.439"}}
 :aliases {:dev {:extra-paths ["resources" "devcards"]
                 :extra-deps {com.bhauman/figwheel-main {:mvn/version "0.2.0-SNAPSHOT"}
                              devcards {:mvn/version "0.2.6"}}}}}
```

Then, crucially, enable devcards in `dev.cljs.edn`, otherwise devcards will be
in noop mode. We also need to tell figwheel about the new build we're exposing.
Edit `dev.cljs.edn`:

```clj
^{:watch-dirs ["src" "devcards"]
  :extra-main-files {:devcards {:main my-app-ns.cards}}} ;; New build
{:main my-app-ns.core
 :devcards true ;; Crucial!
 :optimizations :none
 :pretty-print true
 :source-map true
 :asset-path "/js/dev"
 :output-to "resources/public/js/dev.js"
 :output-dir "resources/public/js/dev"}
```

Restart the figwheel process. Now [http://localhost:9500](http://localhost:9500)
will serve your dev build, and
[http://localhost:9500/figwheel-extra-main/devcards](http://localhost:9500/figwheel-extra-main/devcards)
will serve your devcards.

### Custom Devcards HTML

If you need to serve some CSS files, or otherwise want to tweak the HTML page
that your devcards is served from, add `resources/public/devcards.html` and make
sure it includes `/js/dev-devcards.js`. Then view devcards from
[http://localhost:9500/devcards.html](http://localhost:9500/devcards.html)
instead.

<a id="emacs"></a>
## 5. Run it from Emacs/CIDER

To run it all from Emacs, add a `.dir-locals.el` file to the root of your
project with the following contents:

```clj
((nil
  (cider-clojure-cli-global-options . "-A:dev")
  (cider-default-cljs-repl . figwheel-main)
  (cider-figwheel-main-default-options . ":dev")))
```

This will tell CIDER to use your `:dev` tools.deps profile, to start a
figwheel-main REPL, and to use the `:dev` figwheel build (e.g. `dev.cljs.edn`).

**Pitfall warning**: These vars will be set *when you open new files*, they will
**not** apply when you revisit existing buffers. To avoid frustration, kill the
`deps.edn` buffer, reopen it, and continue from there.

Start a fighweel-main REPL from inside Emacs (remember to close any existing
figwheel process running in a shell) with `C-c M-C-j`, or `cider-jack-in-cljs`.
It's crucial that you use the `cljs` version, and not the `clj` version (note
the added meta in `C-c M-C-j`). I've already wasted enough time running the
wrong jack-in for everyone, no need to repeat this mistake...

You now have the ability to run figwheel-main with Devcards from both Emacs and
the shell. However, your shell users are stuck with a pretty long incantation.
It can be improved by putting the main options into your `deps.edn` file.
However, **do not put `:main-opts` in the profile you intend to use with
CIDER**. It will not work. It will start a figwheel process, and things will
*seem* to be *almost* working, but CIDER will just hang. To avoid this problem,
add a second profile for shell users. As a bonus, throw in
[rebel-readline](https://github.com/bhauman/rebel-readline) for a massively
improved REPL experience on the shell:

```clj
{:paths ["src"]
 :deps {org.clojure/clojure {:mvn/version "1.10.0"}
        org.clojure/clojurescript {:mvn/version "1.10.439"}}
 :aliases {:dev {:extra-paths ["resources" "devcards"]
                 :extra-deps {com.bhauman/figwheel-main {:mvn/version "0.2.0-SNAPSHOT"}
                              devcards {:mvn/version "0.2.6"}}}
           :repl {:extra-deps {com.bhauman/rebel-readline {:mvn/version "0.1.4"}}
                  :main-opts ["-m" "figwheel.main" "-b" "dev" "-r"]}}}
```

Now you can launch a figwheel REPL on the shell with:

```sh
clojure -A:dev -A:repl
```

## 6. In-browser tests

These days I like running tests in two ways: using Bruce Hauman's (yes, him
again, thanks Bruce!)
[cljs-test-display](https://github.com/bhauman/cljs-test-display) to view test
results in a browser (covered here), and
[Kaocha](https://github.com/lambdaisland/kaocha), a command-line test runner
(next section).

To use the visual test display, add the dependency and the `test` directory to
`deps.edn`:

```clj
{:paths ["src"]
 :deps {org.clojure/clojure {:mvn/version "1.10.0"}
        org.clojure/clojurescript {:mvn/version "1.10.439"}}
 :aliases {:dev {:extra-paths ["resources" "devcards" "test"]
                 :extra-deps {com.bhauman/figwheel-main {:mvn/version "0.2.0-SNAPSHOT"}
                              devcards {:mvn/version "0.2.6"}
                              com.bhauman/cljs-test-display {:mvn/version "0.1.1"}}}
           :repl {:extra-deps {com.bhauman/rebel-readline {:mvn/version "0.1.4"}}
                  :main-opts ["-m" "figwheel.main" "-b" "dev" "-r"]}}}
```

Then define another main in `dev.cljs.edn`, and optionally, include a
`:closure-defines` map to
[configure](https://github.com/bhauman/cljs-test-display#configuration) the test
display:

```clj
^{:watch-dirs ["src" "test" "devcards"]
  :extra-main-files {:tests {:main my-app-ns.test-runner}
                     :devcards {:main my-app-ns.cards}}}
{:main my-app-ns.core
 :devcards true
 :optimizations :none
 :pretty-print true
 :source-map true
 :asset-path "/js/dev"
 :output-to "resources/public/js/dev.js"
 :output-dir "resources/public/js/dev"
 :closure-defines {cljs-test-display.core/notifications false
                   cljs-test-display.core/printing false}}
```

The test runner needs to load the library and all your tests. Add
`test/my_app_ns/test_runner.cljs` with the following content:

```clj
(ns ^:figwheel-hooks yahtzee.test-runner
  (:require [my-app-ns.core-test]
            [cljs.test :as test]
            [cljs-test-display.core :as display]))

(enable-console-print!)

(defn test-run []
  (test/run-tests
   (display/init! "app-tests")
   my-app-ns.core-test))

(defn ^:after-load render-on-relaod []
  (test-run))

(test-run)
```

Make sure to require all your tests, and remember to add new ones in here as you
add more. After restarting fighweel, you should see a nice test report on
[http://localhost:9500/figwheel-extra-main/tests](http://localhost:9500/figwheel-extra-main/tests).

## 7. Running tests with Kaocha

Running [Kaocha](https://github.com/lambdaisland/kaocha) with ClojureScript is
almost as easy as running it with Clojure - you just need one additional dep. It
is also convenient to add `:main-opts` to the same profile to make it easy to
run tests:

```clj
{:paths ["src"]
 :deps {org.clojure/clojure {:mvn/version "1.10.0"}
        org.clojure/clojurescript {:mvn/version "1.10.439"}}
 :aliases {:dev {:extra-paths ["test" "resources" "devcards"]
                 :extra-deps {com.bhauman/figwheel-main {:mvn/version "0.2.0-SNAPSHOT"}
                              devcards {:mvn/version "0.2.6"}
                              com.bhauman/cljs-test-display {:mvn/version "0.1.1"}}}
           :repl {:extra-deps {com.bhauman/rebel-readline {:mvn/version "0.1.4"}}
                  :main-opts ["-m" "figwheel.main" "-b" "dev" "-r"]}

           ;; NEW
           :test {:extra-paths ["test"]
                  :extra-deps {lambdaisland/kaocha {:mvn/version "0.0-367"}
                               lambdaisland/kaocha-cljs {:mvn/version "0.0-16"}}
                  :main-opts ["-m" "kaocha.runner" "unit-cljs"]}}}
```

To configure Kaocha, you can (but don't have to) provide a `tests.edn` file in
the root of your project:

```clj
#kaocha/v1
{:tests [{:id :unit-cljs
          :type :kaocha.type/cljs
          :test-paths ["test"]
          :source-paths ["src"]
          ;; :cljs/repl-env cljs.repl.node/repl-env ; this is the default
          ;; :cljs/repl-env cljs.repl.browser/repl-env
          }]
 :reporter [kaocha.report/documentation]}
```

Now you can run tests with Kaocha with:

```clj
clojure -A:test
```

[The kaocha-cljs docs](https://github.com/lambdaisland/kaocha-cljs) has more
information on how to configure and run it.

## 8. Releasing to Clojars

To release a ClojureScript package to Clojars, you need to

1. Provide a `pom.xml`
2. Package the sources in a jar
3. Configure your Clojars credentials with Maven
4. Deploy the two sources to Clojars

This workflow requires maven. If you don't have it:

```sh
brew install maven
```

### 1. Provide a pom.xml

tools.deps can create `pom.xml` for you:

```sh
clojure -Spom
```

Take a look at the generated `pom.xml`, and adjust as appropriate (version,
description, package name etc).

### 2. Package sources in a jar

There are many ways to create a jar. I recommend using
[pack](https://github.com/juxt/pack.alpha). Edit `deps.edn`:

```clj
{:paths ["src"]
 :deps {org.clojure/clojure {:mvn/version "1.10.0"}
        org.clojure/clojurescript {:mvn/version "1.10.439"}}
 :aliases {:dev {:extra-paths ["test" "resources" "devcards"]
                 :extra-deps {com.bhauman/figwheel-main {:mvn/version "0.2.0-SNAPSHOT"}
                              devcards {:mvn/version "0.2.6"}
                              com.bhauman/cljs-test-display {:mvn/version "0.1.1"}}}
           :repl {:extra-deps {com.bhauman/rebel-readline {:mvn/version "0.1.4"}}
                  :main-opts ["-m" "figwheel.main" "-b" "dev" "-r"]}
           :test {:extra-paths ["test"]
                  :extra-deps {lambdaisland/kaocha {:mvn/version "0.0-367"}
                               lambdaisland/kaocha-cljs {:mvn/version "0.0-16"}}
                  :main-opts ["-m" "kaocha.runner" "unit-cljs"]}

           ;; NEW
           :jar {:extra-deps {pack/pack.alpha {:git/url "https://github.com/juxt/pack.alpha.git"
                                               :sha "90a84a01c365fdac224bf4eef6e9c8e1d018a29e"}}
                 :main-opts ["-m" "mach.pack.alpha.skinny" "--no-libs" "--project-path" "my-app.jar"]}}}
```

You should check whatever the latest stable commit is for
[pack](https://github.com/juxt/pack.alpha) and use the corresponding sha. Also
change `my-app.jar` to a suitable name. You can now create a jar with:

```sh
clojure -A:jar
```

### 3. Configure Clojars credentials for maven

Open or create `~/.m2/settings.xml`, and add your Clojars username and password
to it:

```xml
<settings>
  <servers>
    <server>
      <id>clojars</id>
      <username>username</username>
      <password>password</password>
    </server>
  </servers>
</settings>
```

### 4. Deploy to Clojars

With all the pieces in place, you can now release your library:

```sh
mvn deploy:deploy-file \
    -Dfile=my-app.jar \
    -DrepositoryId=clojars \
    -Durl=https://clojars.org/repo \
    -DpomFile=pom.xml
```

I suggest adding `pom.xml` to git to persist whatever metadata you added to it.
When you have a new version, make sure to update any dependency information,
rebuild the jar and deploy:

```sh
clojure -Spom
clojure -A:jar

mvn deploy:deploy-file \
    -Dfile=my-app.jar \
    -DrepositoryId=clojars \
    -Durl=https://clojars.org/repo \
    -DpomFile=pom.xml
```

## 9. Add a Makefile

tools.deps isn't really a build-system, although its `:main-opts` will allow you
to solve most uses with just your `deps.edn` file. However, I usually top the
whole thing off with a Makefile. Explaining it is outside the scope of this
article though, so I'll just provide an example for you to look at, and
encourage you to [learn more about this fascinating
tool](https://gist.github.com/isaacs/62a2d1825d04437c6f08) if you aren't already
using it:

```makefile
test:
	clojure -A:test

my-app.jar: src/**/*
	clojure -A:jar

pom.xml:
	clojure -Spom

deploy: pom.xml test my-app.jar
	mvn deploy:deploy-file \
	    -Dfile=my-app.jar \
	    -DrepositoryId=clojars \
	    -Durl=https://clojars.org/repo \
	    -DpomFile=pom.xml

.PHONY: test deploy
```
