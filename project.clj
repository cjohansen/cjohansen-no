(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "2.2.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]
                 [enlive "1.1.5"]
                 [clygments "0.1.1"]
                 [optimus "0.14.2"]]
  :ring {:handler cjohansen-no.web/app}
  :aliases {"build-site" ["run" "-m" "cjohansen-no.web/export"]}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}
             :test {:dependencies [[midje "1.6.0"]]
                    :plugins [[lein-midje "3.1.3"]]}}
  :jvm-opts ["-Djava.awt.headless=true"])
