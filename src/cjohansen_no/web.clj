(ns cjohansen-no.web
  (:require [cjohansen-no.ingest :as ingest]
            [cjohansen-no.page :as page]
            [cjohansen-no.tech-blog :as tech]
            [clojure.string :as str]
            [datomic.api :as d]
            [optimus.assets :as assets]
            optimus.export
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [stasis.core :as stasis]))

(defn wrap-utf-8
  "This function works around the fact that Ring simply chooses the default JVM
  encoding for the response encoding. This is not desirable, we always want to
  send UTF-8."
  [handler]
  (fn [request]
    (when-let [response (handler request)]
      (if (.contains (get-in response [:headers "Content-Type"]) ";")
        response
        (if (string? (:body response))
          (update-in response [:headers "Content-Type"] #(str % "; charset=utf-8"))
          response)))))

(defn get-assets []
  (assets/load-assets "public" [#".*\.css$"
                                #".*\.png$"
                                #".*\.svg$"
                                #".*\.ico$"
                                #".*\.js$"]))

(defn ingest-and-render-tech [conn post req]
  (let [url (str/replace (:uri req) #"\/index\.html$" "/")
        path (str "tech" (str/replace url #"/$" ".md"))]
    (d/transact conn [[:db/retractEntity (:db/id post)]])
    (d/transact conn (ingest/tech-post-txes path))
    (tech/render-page req (tech/find-by-url (d/db conn) url))))

(defn tech-pages [conn]
  (let [posts (tech/load-posts (d/db conn))]
    (zipmap (map :browsable/url posts)
            (map #(fn [req] (ingest-and-render-tech conn % req)) posts))))

(defn tech-tag-pages [db]
  (let [tags (tech/load-tags db)]
    (zipmap (map tech/tag-url tags)
            (map #(fn [req] (tech/tag-page req %)) tags))))

(defn get-raw-pages []
  (let [conn (ingest/db-conn)]
    (stasis/merge-page-sources
     {:public (stasis/slurp-directory "resources/public" #".*\.(html)$")
      :tech-pages (tech-pages conn)
      :tech-tag-pages (tech-tag-pages (d/db conn))})))

(defn prepare-page [page req]
  (-> (if (string? page) page (page req))
      page/finalize-page))

(defn prepare-pages [pages]
  (zipmap (keys pages)
          (map #(partial prepare-page %) (vals pages))))

(defn get-pages []
  (prepare-pages (get-raw-pages)))

(defn index [conn]
  (when-let [txes (seq (ingest/ingest-everything (d/db conn)))]
    (println "Ingesting" (count (into [] cat txes)) "txes")
    (doseq [tx-data txes]
      (d/transact conn tx-data))))

(defn app-handler []
  (let [conn (ingest/db-conn)]
    (index conn)
    (-> (stasis/serve-pages get-pages)
        (optimus/wrap get-assets optimizations/none serve-live-assets)
        wrap-content-type
        wrap-utf-8)))

(def export-dir "build")

(defn export []
  (let [assets (optimizations/all (get-assets) {})]
    (index (ingest/db-conn))
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets})))

(comment
  (index (ingest/db-conn))
  (tech/load-posts (d/db (ingest/db-conn)))

  (def tag (first (tech/load-tags (d/db (ingest/db-conn)))))
  (:tech-blog/_tags tag)

)
