(ns cjohansen-no.markdown
  (:import [com.vladsch.flexmark.html HtmlRenderer]
           [com.vladsch.flexmark.parser Parser]
           [com.vladsch.flexmark.ext.tables TablesExtension]
           [com.vladsch.flexmark.util.data MutableDataSet]))

(def opts (-> (MutableDataSet.)
              (.set Parser/EXTENSIONS [(TablesExtension/create)])))

(defn to-html [s]
  (->> (.parse (.build (Parser/builder opts)) s)
       (.render (.build (HtmlRenderer/builder opts)))))
