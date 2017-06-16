(ns cjohansen-no.web-test
  (:require [cjohansen-no.web :as web]
            [clojure.test :refer [testing is]]
            [net.cgrand.enlive-html :as enlive]))

;; (testing "All pages respond with 200 OK"
;;   (doseq [url (keys (web/get-pages))]
;;     (let [status (:status (web/app {:uri url}))]
;;       (is (= [url status] [url 200])))))

;; (defn link-valid? [pages link]
;;   (let [href (get-in link [:attrs :href])]
;;     (or
;;      (not (.startsWith href "/"))
;;      (contains? pages href)
;;      (contains? pages (str href "index.html")))))

;; (testing "All links are valid"
;;   (let [pages (web/get-pages)]
;;     (doseq [url (remove #(re-find #"\.(js|css)$" %) (keys (web/get-pages)))
;;             link (-> (:body (web/app {:uri url}))
;;                      java.io.StringReader.
;;                      enlive/html-resource
;;                      (enlive/select [:a]))]
;;       (when-let [href (get-in link [:attrs :href])]
;;         (is (= [url href (link-valid? pages link)] [url href true]))))))
