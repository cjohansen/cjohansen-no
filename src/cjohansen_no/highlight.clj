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
  (enlive/sniptest page
                   [:pre :code] highlight
                   [:pre :code] #(assoc-in % [:attrs :class] "codehilite")))
