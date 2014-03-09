(ns cjohansen-no.web-test
  (:require [cjohansen-no.web :refer :all]
            [clojure.pprint :as pp]
            [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as enlive]))

(fact
 "All pages respond with 200 OK"

 (doseq [url (keys (get-pages))]
   (let [status (:status (app {:uri url}))]
     [url status] => [url 200])))

(defn link-valid? [pages link]
  (let [href (get-in link [:attrs :href])]
    (or
     (not (.startsWith href "/"))
     (contains? pages href)
     (contains? pages (str href "index.html")))))

(fact
 "All links are valid"

 (let [pages (get-pages)]
   (doseq [url (keys (get-pages))]
     (doseq [link (-> (:body (app {:uri url}))
                      java.io.StringReader.
                      enlive/html-resource
                      (enlive/select [:a]))]
       (let [href (get-in link [:attrs :href])]
         [url href (link-valid? pages link)] => [url href true])))))
