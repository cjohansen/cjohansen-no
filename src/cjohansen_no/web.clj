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

(defn- current-year []
  (+ 1900 (.getYear (java.util.Date.))))

(defn layout-page [request page]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "chrome=1"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "edge"}]
    [:meta {:name "author" :content "Christian Johansen"}]
    [:title "Tech blog"]
    [:link {:rel "stylesheet" :href (link/file-path request "/stylesheets/cjohansen.css")}]]
   [:body
    [:div.banner.masthead
     [:p
      [:a {:href "/"}
       [:strong "cjohansen.no"]
       "Programming, free software"]]]
    [:div.article page]
    [:div.banner.footer
     [:p [:span
          [:a {:rel "license"
               :href "http://creativecommons.org/licenses/by-nc-sa/3.0/"
               :title "Creative Commons License"}
           [:img {:alt "Creative Commons License" :src "/images/cc-by-nc-sa.png"}]]
          "2006 - "
          (current-year)
          [:a {:href "mailto:christian@cjohansen.no"} "Christian Johansen"]]]]
    [:script "var _gaq=_gaq||[];_gaq.push(['_setAccount','UA-20457026-1']);_gaq.push(['_trackPageview']);(function(b){var c=b.createElement('script');c.type='text/javascript';c.async=true;c.src='http://www.google-analytics.com/ga.js';var a=b.getElementsByTagName('script')[0];a.parentNode.insertBefore(c,a)})(document);"]]))

(def pegdown-options ;; https://github.com/sirthias/pegdown
  [:autolinks :fenced-code-blocks :strikethrough])

(defn markdown-pages [pages]
  (zipmap (map #(str/replace % #"\.md$" "/") (keys pages))
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

(def export-dir "build")

(defn export []
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets})))
