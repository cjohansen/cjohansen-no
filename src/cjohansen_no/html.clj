(ns cjohansen-no.html
  (:require [clojure.string :as str]
            [dumdom.string :as dumdom]
            [optimus.link :as link]))

(def pegdown-options ;; https://github.com/sirthias/pegdown
  [:autolinks :fenced-code-blocks :strikethrough])

(defn- current-year []
  (+ 1900 (.getYear (java.util.Date.))))

(defn styles [attrs]
  (->> attrs
       (map (fn [[k v]] (str (name k) ": " v)))
       (str/join ";")))

(defn html5 [& args]
  (str "<!DOCTYPE html>" (dumdom/render [:html args])))

(defn layout-page [request page & [opt]]
  (let [ferm-active? (str/starts-with? (:uri request) "/fermentations")]
    (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1.0"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "chrome=1"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "edge"}]
      [:meta {:name "author" :content "Christian Johansen"}]
      [:title (or (:page-title opt) "Tech blog")]
      [:link {:rel "stylesheet" :href (link/file-path request "/styles/main.css")}]]
     [:body
      [:div.banner.masthead.main-content.vs-s
       [:p {:style (styles {:display "flex"
                            :justify-content "space-between"})}
        [:a.banner-link {:href "/"} "Christian Johansen"]
        [:span
         [:a.banner-link {:href "/"
                          :style (styles
                                  {:color (when ferm-active? "#999")
                                   :font-weight "normal"
                                   :padding-right "10px"
                                   :border-right "1px solid #ebe8e3"
                                   :margin-right "10px"})} "Tech"]
         [:a.banner-link {:style (styles {:color (when-not ferm-active? "#999")
                                          :font-weight "normal"})
                          :href "/fermentations/"} "Fermentations"]]]
       [:hr]
       (when (or (:prev opt) (:next opt))
         [:div
          [:p {:style (styles {:display "flex"
                               :justify-content "space-between"
                               :font-size "0.9rem"})}
           (when-let [prev (:prev opt)]
             [:a {:href (:url prev)
                  :style (styles
                          {:flex "50% 0 0"})} "< " (:title prev)])
           (when-let [next (:next opt)]
             [:a {:href (:url next)
                  :style (styles
                          {:flex "50% 0 0"
                           :text-align "right"})} (:title next) " >"])
           [:hr]]])]
      [:div.main-content
       page
       (when-let [f (:page-fn opt)]
         (f request))]
      [:div.banner.footer.main-content
       [:hr]
       [:p.related
        [:a.item {:rel "license"
                  :href "http://creativecommons.org/licenses/by-nc-sa/3.0/"
                  :title "Creative Commons License"}
         [:img {:alt "Creative Commons License" :src "/images/cc-by-nc-sa.png"}]]
        [:span.item "2006 - " (current-year)]
        [:a.item {:href "mailto:christian@cjohansen.no"} "Christian Johansen"]]]
      [:script "var _gaq=_gaq||[];_gaq.push(['_setAccount','UA-20457026-1']);_gaq.push(['_trackPageview']);(function(b){var c=b.createElement('script');c.type='text/javascript';c.async=true;c.src='http://www.google-analytics.com/ga.js';var a=b.getElementsByTagName('script')[0];a.parentNode.insertBefore(c,a)})(document);"]])))

(defn layout-page-new [request page]
  (let [base-url (str (name (:scheme request)) "://" (get-in request [:headers "host"]))]
    (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:meta {:name "author" :content "Christian Johansen"}]

      (when-let [title (:open-graph/title page)]
        [:meta {:property "og:title" :content title}])
      [:meta {:property "og:type" :content (or (:open-graph/type page) "article")}]
      [:meta {:property "og:url" :content (str base-url (:uri request))}]
      (when-let [image (:open-graph/image page)]
        [:meta {:property "og:image" :content (str base-url (:image/url image))}])
      (when-let [description (:open-graph/description page)]
        [:meta {:property "og:description" :content description}])
      [:title (or (:page-title page) "Tech blog")]
      [:link {:rel "stylesheet" :href (link/file-path request "/css/cjohansen.css")}]]
     [:body
      (:body page)])))
