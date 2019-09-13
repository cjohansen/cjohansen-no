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

(defn section [{:keys [title sub-title content class]}]
  [:div.section {:className class}
   [:div.content
    (when title (h1 {} title))
    (when sub-title (h2 {} sub-title))
    content]])

(defn centered [params]
  (section (assoc params :class "centered")))

(defn header []
  [:div.header
   [:div.logo
    [:div.firstname [:span.first-letter "C"] [:span.rest "hristian"]]
    [:div.lastname "Johansen"]]
   [:ul.nav-list.menu {:role "nav"}
    [:li [:a {:href "/"} "Tech"]]
    [:li [:a {:href "/fermentations/"} "Fermentations"]]
    [:li [:a {:href "/about/"} "About" [:span.extended" me"]]]]])
