(ns cjohansen-no.web
  (:require [optimus.assets :as assets]
            [optimus.export]
            [optimus.link :as link]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [cjohansen-no.highlight :refer [highlight-code-blocks]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
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

(defn layout-page [request page]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:title "Tech blog"]
    [:link {:rel "stylesheet" :href (link/file-path request "/styles/main.css")}]]
   [:body
    [:div.logo "cjohansen.no"]
    [:div.body page]]))

(def pegdown-options ;; https://github.com/sirthias/pegdown
  [:autolinks :fenced-code-blocks :strikethrough])

(defn markdown-pages [pages]
  (zipmap (map #(str/replace % #"\.md$" "") (keys pages))
          (map #(fn [req] (layout-page req (md/to-html % pegdown-options)))
               (vals pages))))

(defn archived-pages [pages]
  pages)

(defn get-raw-pages []
  (stasis/merge-page-sources
   {:public (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
    :markdown (markdown-pages (stasis/slurp-directory "resources/md" #"\.md$"))}))

(defn prepare-page [page req]
  (-> (if (string? page) page (page req))
      highlight-code-blocks))

(defn prepare-pages [pages]
  (zipmap (keys pages)
          (map #(partial prepare-page %) (vals pages))))

(defn get-pages []
  (stasis/merge-page-sources
   {:new-pages (prepare-pages (get-raw-pages))
    :wget-archive (archived-pages (stasis/slurp-directory "resources/wget-archive" #".*\..*"))}))

(def app (-> (stasis/serve-pages get-pages)
             (optimus/wrap get-assets optimizations/all serve-live-assets)
             wrap-content-type
             wrap-utf-8))

(def export-dir "dist")

(defn export []
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets})))
