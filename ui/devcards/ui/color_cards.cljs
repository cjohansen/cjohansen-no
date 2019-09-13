(ns ui.color-cards
  (:require [dumdom.devcards :refer-macros [defcard]]))

(def color-names
  ["ruby"
   "lima"
   "denim"
   "fulvors"
   "athens"
   "mystic"
   "lightbw"
   "smoky"
   "jet"])

(def fg-colors
  {"snow" "var(--jet)"
   "athens" "var(--jet)"
   "lightbw" "var(--jet)"
   "mystic" "var(--jet)"})

(defn color [name]
  [:div {:style {:border "1px solid #ddd"
                 :background (str "var(--" name ")")
                 :padding-bottom "30%"
                 :border-radius "2px"
                 :position "relative"}}
   [:div {:style {:position "absolute"
                  :top "50%"
                  :left "50%"
                  :font-size "18px"
                  :transform "translate(-50%, -50%)"
                  :color (get fg-colors name "#fff")}}
    name]])

(defcard colors
  [:div {:style {:display "grid"
                 :grid-template-columns "1fr 1fr 1fr 1fr"
                 :grid-gap "10px"}}
   (map color color-names)])
