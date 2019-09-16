(ns ui.elements
  (:require [clojure.string :as str]))

(defn el [el params text]
  [(get params :element el)
   {:className (str (name el)
                    (when-let [cn (:className params)]
                      (str " " cn)))} text])

(def h1 (partial el :h1))
(def h2 (partial el :h2))
(def h3 (partial el :h3))
(def h4 (partial el :h4))

(defn section [{:keys [title sub-title content class media]}]
  [:div.section {:className class}
   [:div.content
    (when media
      [:div.media
       media])
    [:div.section-content.text-content
     (when title (h1 {} title))
     (when sub-title (h2 {} sub-title))
     content]]])

(defn centered [params]
  (section (assoc params :class "centered")))

(defn section-media-front [params]
  (section (assoc params :class "media-front")))

(defn section-media-back [params]
  (section (assoc params :class "media-back")))

(defn header []
  [:div.header
   [:div.logo
    [:div.facebox
     [:img.img {:src "/images/christian-bw.png" :width 100}]]
    [:div.logo-name
     [:div.logo-firstname "Christian"]
     [:div.logo-lastname "Johansen"]]]
   [:ul.nav-list.menu {:role "nav"}
    [:li [:a {:href "/"} "Tech"]]
    [:li [:a {:href "/fermentations/"} "Fermentations"]]
    [:li [:a {:href "/about/"} "About" [:span.hide-mobile " me"]]]]])

(defn blockquote [quote & [source]]
  [:blockquote.bq.text-content
   (when source
     [:div.bq-source
      [:p source]])
   [:div.bq-quote
    [:p quote]]])

(defn ingredient-list [ingredients]
  [:table.table
   (for [{:keys [amount percent temp title]} ingredients]
     [:tr
      [:th title (when temp [:span.subtle (str " (" temp ")")])]
      [:td amount]
      [:td [:strong percent]]
      ])])

(defn byline [{:keys [title date tags]}]
  [:div
   [:h2 title]
   [:p.byline
    [:span.date date]
    (when tags
      [:span.subtle (->> (for [{:keys [title url]} tags]
                           [:a {:href url} title])
                         (interpose ", "))])]])
