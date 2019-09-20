rin(ns cjohansen-no.highlight
  (:require [clojure.java.io :as io]
            [clygments.core :as pygments]
            [net.cgrand.enlive-html :as enlive]
            [clojure.string :as str]))

(defn- extract-code
  [highlighted]
  (-> highlighted
      java.io.StringReader.
      enlive/html-resource
      (enlive/select [:pre])
      first
      :content))

(defn highlight [node]
  (let [code (->> node :content (apply str) str/trim)
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

(defn try-highlight [node]
  (if (and (= 1 (count (:content node)))
           (= :code (-> node :content first :tag)))
    (-> node
        (update-in [:content 0] highlight)
        (assoc-in [:attrs :class] "codehilite"))
    node))

(defn highlight-code-blocks [page]
  (if (string? page)
    (enlive/sniptest page
                     [:pre] try-highlight
                     ;;[:pre :code] highlight
                     ;;[:pre :code] #(assoc-in % [:attrs :class] "codehilite")
                     )
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

  (-> "(defproject cjohansen-no \"0.1.0-SNAPSHOT\"
  :description \"cjohansen.no source code\"
  :url \"http://cjohansen.no\"
  :license {:name \"BSD 2 Clause\"
            :url \"http://opensource.org/licenses/BSD-2-Clause\"}
  :dependencies [[org.clojure/clojure \"1.5.1\"]
                 [stasis \"1.0.0\"]])"
      (pygments/highlight :clj :html)
      prn)

  (-> "lein new cjohansen-no
cd cjohansen-no"
      (pygments/highlight :sh :html)
      prn)



  (-> (slurp (clojure.java.io/file "/Users/christian/projects/hafslund/consumptor/recipients.js"))
      (pygments/highlight :js :html)
      ;;hicv/html->hiccup
      prn)
  )
