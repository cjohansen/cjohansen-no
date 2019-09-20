(ns cjohansen-no.html-walker
  (:refer-clojure :exclude [replace find])
  (:import [ch.digitalfondue.jfiveparse Document Element Node Parser Selector NodeMatcher]))

(defn create-matcher [path]
  (.toMatcher
   (reduce (fn [selector element-kw]
             (-> selector
                 .withChild
                 (.element (name element-kw))))
           (-> (Selector/select)
               (.element (name (first path))))
           (next path))))

(defn replace [html path->f]
  (let [parser (Parser.)
        doc (.parse parser html)]
    (doseq [[path f] path->f]
      (doseq [node (.getAllNodesMatching doc (create-matcher path))]
        (f node)))
    (.getOuterHTML doc)))

(defn find [html path]
  (.getAllNodesMatching (.parse (Parser.) html) (create-matcher path)))
