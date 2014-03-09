# My first post

It's pretty short for now. Here's our project.clj:

```clj
(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]]
  :ring {:handler cjohansen-no.web/app}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})
```

[About](/about/)
