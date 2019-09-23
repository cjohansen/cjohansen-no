(ns ui.elements-cards
  (:require [dumdom.devcards :refer-macros [defcard]]
            [dumdom.core :as dd]
            [dumdom.dom :as d]
            [ui.elements :as e]))

(defcard h1
  (e/h1 {} "Clojure in Production with tools.deps"))

(defcard h2-acting-as-h1
  (e/h1 {:element :h2} "Clojure in Production with tools.deps"))

(defcard h2
  (e/h2 {} "Tying it all together"))

(defcard h1-acting-as-h2
  (e/h2 {:element :h1} "Some headings are quite long and will wrap - when they do they should still be legible"))

(defcard h3
  (e/h3 {} "Tying it all together"))

(defcard h1-acting-as-h3
  (e/h3 {:element :h1} "Some headings are quite long and will wrap - when they do they should still be legible"))

(defcard h4
  (e/h4 {} "Tying it all together"))

(defcard h1-acting-as-h4
  (e/h4 {:element :h1} "Some headings are quite long and will wrap - when they do they should still be legible"))

(defcard paragraphs
  [:p {} "I'm hoping this post will not only show you the power of Clojure and Stasis for building static web sites, but also give you a good introduction to some very useful Clojure libraries. Maybe even to Clojure itself. In particular, I will discuss using these libraries: Stasis, Optimus, enlive, hiccup, cegdown, clygments and even write some tests with Midje."])

(defcard unordered-list
  [:ul {}
   [:li "Some things"]
   [:li "In a nice short list"]
   [:li "Wow, those are nice things"]])

(defcard ordered-list
  [:ol {}
   [:li "Some things"]
   [:li "In a nice short list"]
   [:li "Wow, those are nice things"]])

(defcard blockquote
  (e/blockquote
   "Throughput and IOPS scale as a file system grows and can burst to higher
   throughput levels for short periods of time to support the unpredictable
   performance needs of file workloads. For the most demanding workloads, Amazon
   EFS can support performance over 10 GB/sec and up to 500,000 IOPS."
   "AWS Docs"))

(defcard dark-blockquote
  [:div.dark-theme1
   (e/section
     {:content
      (e/blockquote
       "Throughput and IOPS scale as a file system grows and can burst to higher
   throughput levels for short periods of time to support the unpredictable
   performance needs of file workloads. For the most demanding workloads, Amazon
   EFS can support performance over 10 GB/sec and up to 500,000 IOPS."
       "AWS Docs")})])

(defcard blockquote-no-source
  (e/blockquote
   "Throughput and IOPS scale as a file system grows and can burst to higher
   throughput levels for short periods of time to support the unpredictable
   performance needs of file workloads. For the most demanding workloads, Amazon
   EFS can support performance over 10 GB/sec and up to 500,000 IOPS."))

(defcard header
  (e/header))

(defcard dark-header
  [:div.dark-theme1 (e/header)])

(defcard ingredient-list
  (e/ingredient-list
   [{:amount "250 g" :percent "25%" :title [:a {:href "#"} "Coarse whole wheat flour"]}
    {:amount "250 g" :percent "25%" :title "White wheat flour"}
    {:amount "100 g" :percent "10%" :title "Flaked oats"}
    {:amount "200 g" :percent "20%" :title "Milk" :temp "38℃"}
    {:amount "100 g" :percent "10%" :title "Water" :temp "38℃"}
    {:amount "50 g" :percent "5%" :title "Soft butter"}
    {:amount "50 g" :percent "5%" :title "Sugar"}
    {:amount "22 g" :percent "2.2%" :title "Salt"}
    {:amount "3 g" :percent "0.3%" :title "Yeast"}]))

(defcard byline
  [:div
   (e/h2 {} "Variations on Flour Water Salt Yeast 50% Whole Wheat")
   (e/byline
    {:published "September 01 2019"
     :tags [{:title "Flax" :url "/tags/flax"}
            {:title "Bread" :url "/tags/bread"}
            {:title "Sesame" :url "/tags/sesame"}
            {:title "Whole wheat" :url "/tags/whole-wheat"}
            {:title "FWSY" :url "/tags/fwsy"}]})])

(defcard byline-updated
  [:div
   (e/h2 {} "Variations on Flour Water Salt Yeast 50% Whole Wheat")
   (e/byline
    {:published "September 1st"
     :updated "September 3rd 2019"
     :tags [{:title "Flax" :url "/tags/flax"}
            {:title "Bread" :url "/tags/bread"}
            {:title "Sesame" :url "/tags/sesame"}
            {:title "Whole wheat" :url "/tags/whole-wheat"}
            {:title "FWSY" :url "/tags/fwsy"}]})])

(defcard image-with-legend-red
  [:div {:style {:display "grid"
                 :gridTemplateColumns "1fr 1fr"}}
   (e/captioned-image
    {:src "/devcard_images/IMG_1620.jpg"
     :caption "A sunflower seed bread, cooked in a dutch oven"})
   (e/captioned-image
    {:src "/devcard_images/IMG_1620.jpg"
     :theme :blue
     :caption "A sunflower seed bread, cooked in a dutch oven"})
   (e/captioned-image
    {:src "/devcard_images/IMG_1620.jpg"
     :theme :green
     :caption "A sunflower seed bread, cooked in a dutch oven"})
   (e/captioned-image
    {:src "/devcard_images/IMG_1620.jpg"
     :theme :light
     :caption "A sunflower seed bread, cooked in a dutch oven"})
   (e/captioned-image
    {:src "/devcard_images/IMG_1620.jpg"
     :pop? true
     :caption "A sunflower seed bread, cooked in a dutch oven"})
   (e/captioned-image
    {:src "/devcard_images/IMG_1620.jpg"
     :theme :blue
     :pop? true
     :caption "A sunflower seed bread, cooked in a dutch oven"})
   (e/captioned-image
    {:src "/devcard_images/IMG_1620.jpg"
     :theme :green
     :pop? true
     :caption "A sunflower seed bread, cooked in a dutch oven"})
   (e/captioned-image
    {:src "/devcard_images/IMG_1620.jpg"
     :theme :light
     :pop? true
     :caption "A sunflower seed bread, cooked in a dutch oven"})])

(defcard teaser
  (e/teaser
   {:media [:img.img {:src "/devcard_images/IMG_1555.jpg"}]
    :title "Variations on Flour Water Salt Yeast 50% Whole Wheat"
    :published "September 1st 2019"
    :url "/fermentations/2019-09-01-fwsy-5050/"}))

(defcard teaser-shorter-text
  (e/teaser
   {:media [:img.img {:src "/devcard_images/IMG_1603.jpg"}]
    :title "Flour Water Salt Yeast 50% Whole Wheat"
    :published "August 31st 2019"
    :pitch "Previous"
    :url "/fermentations/2019-08-31-fwsy-5050/"}))
