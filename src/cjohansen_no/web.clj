(ns cjohansen-no.web
  (:require [cjohansen-no.fermentations :as fermentations]
            [cjohansen-no.highlight :refer [highlight-code-blocks]]
            [cjohansen-no.html :as html]
            [cjohansen-no.ingest :as ingest]
            [cjohansen-no.tech-blog :as tech]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [datomic.api :as d]
            [me.raynes.cegdown :as md]
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
  (assets/load-assets "public" [#".*"]))

(defn markdown-pages [pages]
  (zipmap (map #(str/replace % #"\.md$" "/") (keys pages))
          (map #(fn [req] (html/layout-page req (md/to-html % html/pegdown-options)))
               (vals pages))))

(defn tech-pages [conn]
  (let [posts (tech/load-posts (d/db conn))]
    (zipmap (map :browsable/url posts)
            (map tech/render-page posts))))

(defn get-raw-pages []
  (let [fermentations (fermentations/load-fermentations (stasis/slurp-directory "resources/fermentations" #"\.md$"))
        conn (ingest/db-conn)]
    (stasis/merge-page-sources
     {:public (stasis/slurp-directory "resources/public" #".*\.(html)$")
      :bread-images (stasis/slurp-directory "resources/public/images/bread" #".\.jpg$")
      ;;:markdown (markdown-pages (stasis/slurp-directory "resources/md" #"\.md$"))
      :tech-pages (tech-pages conn)
      :fermentation-pages (fermentations/prepare-pages fermentations)
      :frontpage {"/index.html" #(html/layout-page % (md/to-html (slurp (io/resource "index.md")) html/pegdown-options) {:page-title "Christian Johansen"})}
      :fermentations {"/fermentations/" #(html/layout-page % (md/to-html (slurp (io/resource "fermentations.md")) html/pegdown-options)
                                                           {:page-title "Fermentations - Christian Johansen"
                                                            :page-fn (partial fermentations/render-blurbs fermentations)})}
      :ferm-tags (->> fermentations
                      (mapcat :tags)
                      distinct
                      (map #(fermentations/prepare-tag-page % fermentations))
                      (into {}))
      })))

(defn prepare-page [page req]
  (-> (if (string? page) page (page req))
      highlight-code-blocks))

(defn prepare-pages [pages]
  (zipmap (keys pages)
          (map #(partial prepare-page %) (vals pages))))

(defn get-pages []
  (prepare-pages (get-raw-pages)))

(def app (-> (stasis/serve-pages get-pages)
             (optimus/wrap get-assets optimizations/none serve-live-assets)
             wrap-content-type
             wrap-utf-8))

(def export-dir "build")

(defn export []
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets})))
