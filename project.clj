(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/core.match "0.3.0"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [mapdown "0.2.1"]
                 [stasis "2.3.0"]
                 [ring "1.6.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]
                 [enlive "1.1.6"]
                 [clygments "1.0.0"]
                 [optimus "0.19.3"]
                 [cjohansen/dumdom "2019.09.16"]
                 [ch.digitalfondue.jfiveparse/jfiveparse "0.5.3"]]
  :ring {:handler cjohansen-no.web/app}
  :aliases {"build-site" ["run" "-m" "cjohansen-no.web/export"]}
  :profiles {:dev {:dependencies [[hiccup-bridge "1.0.1"]]
                   :plugins [[lein-ring "0.12.0"]]}}
  :jvm-opts ["-Djava.awt.headless=true"])
