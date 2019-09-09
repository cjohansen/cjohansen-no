(ns cjohansen-no.highlight
  (:require [clojure.java.io :as io]
            [clygments.core :as pygments]
            [net.cgrand.enlive-html :as enlive]))

(defn- extract-code
  [highlighted]
  (-> highlighted
      java.io.StringReader.
      enlive/html-resource
      (enlive/select [:pre])
      first
      :content))

(defn- highlight [node]
  (let [code (->> node :content (apply str))
        lang (->> node :attrs :class keyword)
        pygments-info (->> node :attrs :data-pygments)]

    (if (= pygments-info "ignore")
      node
      (try
        (assoc node :content (-> code
                                 (pygments/highlight lang :html)
                                 extract-code))
        (catch Throwable e
          (println (format "Failed to highlight %s code snippet!" lang))
          (println code))))))

(defn highlight-code-blocks [page]
  (if (string? page)
    (enlive/sniptest page
                     [:pre :code] highlight
                     [:pre :code] #(assoc-in % [:attrs :class] "codehilite"))
    page))

(comment
  (highlight-code-blocks
   "<pre><code class=\"clj\">(defproject cjohansen-no \"0.1.0-SNAPSHOT\"
  :description \"cjohansen.no source code\"
  :url \"http://cjohansen.no\"
  :license {:name \"BSD 2 Clause\"
            :url \"http://opensource.org/licenses/BSD-2-Clause\"}
  :dependencies [[org.clojure/clojure \"1.5.1\"]
                 [stasis \"1.0.0\"]
                 [ring \"1.2.1\"]]
  :ring {:handler cjohansen-no.web/app}
  :profiles {:dev {:plugins [[lein-ring \"0.8.10\"]]}})</code></pre>")

  (highlight-code-blocks
   (slurp (clojure.java.io/file "/tmp/stuff.html")))

  (require '[hiccup-bridge.core :as hicv])

  (-> (slurp (clojure.java.io/file "/tmp/stuff.html"))
      (pygments/highlight :html :html)
      hicv/html->hiccup
      prn)
  )
