(ns cjohansen-no.web
  (:require [cjohansen-no.ingest :as ingest]
            [cjohansen-no.live-reload :refer [wrap-live-reload]]
            [cjohansen-no.page :as page]
            [cjohansen-no.tech-blog :as tech]
            [cjohansen-no.fermentation-blog :as ferments]
            [clojure.string :as str]
            [datomic.api :as d]
            [optimus.assets :as assets]
            optimus.export
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [stasis.core :as stasis]))

(defn header-key [res n]
  (let [header-name (str/lower-case n)]
    (->> (keys (:headers res))
         (filter #(= header-name (str/lower-case %)))
         first)))

(defn make-utf-8 [res]
  (when res
    (let [k (header-key res "Content-Type")
          content-type (get-in res [:headers k])]
      (if (or (nil? k)
              (empty? content-type)
              (.contains content-type ";")
              (not (string? (:body res))))
        res
        (update-in res [:headers k] #(str % "; charset=utf-8"))))))

(defn wrap-utf-8
  "This function works around the fact that Ring simply chooses the default JVM
  encoding for the response encoding. This is not desirable, we always want to
  send UTF-8."
  [handler]
  (fn middleware
    ([req] (-> req handler make-utf-8))
    ([req respond raise]
     (handler req #(-> % make-utf-8 respond) raise))))

(defn get-assets []
  (assets/load-assets "public" [#".*\.css$"
                                #".*\.png$"
                                #".*\.svg$"
                                #".*\.ico$"
                                #".*\.js$"]))

(defn database-pages [db]
  (let [pages (->> (d/q '[:find ?e
                          :in $
                          :where
                          [?e :browsable/url]]
                        db)
                   (map #(d/entity db (first %))))]
    (zipmap (map :browsable/url pages)
            (map #(fn [req]
                    (case (:browsable/kind %)
                      :page/tech-post (tech/render-page req %)
                      :page/tech-tag (tech/tag-page req %)
                      :page/bread-post (ferments/render-page req %))) pages))))

(defn get-raw-pages []
  (let [conn (ingest/db-conn)
        db (d/db conn)]
    (stasis/merge-page-sources
     {:public (stasis/slurp-directory "resources/public" #".*\.(html)$")
      :db-pages (database-pages db)})))

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
        (wrap-live-reload {:before-reload (fn [e] (index (ingest/db-conn)))})
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
