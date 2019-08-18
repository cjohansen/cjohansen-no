(ns cjohansen-no.fermentations
  (:require [cjohansen-no.html :as html]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.core :as hiccup]
            [mapdown.core :as mapdown]
            [me.raynes.cegdown :as md]))

(defn load-fermentations [ferms]
  (let [ferms (->> ferms
                   (map (fn [[path v]]
                          (-> (mapdown/parse v)
                              (assoc :url (str "/fermentations" (str/replace path #"\.md$" "/")))
                              (update :body md/to-html html/pegdown-options)
                              (update :published #(when % (java.time.LocalDate/parse %)))
                              (update :type read-string)
                              (update :tags #(and % (read-string %)))))))]
    (concat
     (->> ferms
          (remove #(= :bread (:type %)))
          (sort-by :published)
          (map-indexed #(assoc %2 :batch (str "Beer #" (inc %1)))))
     (->> ferms
          (filter #(= :bread (:type %)))
          (sort-by :published)
          (map-indexed #(assoc %2 :batch (str "Bread #" (inc %1))))))))

(defn tag-name [tag]
  (str/join " " (str/split (str/capitalize (name tag)) #"-")))

(defn byline [{:keys [published tags]}]
  (hiccup/html
   [:p {:style (html/styles
                {:color "#999"
                 :margin-top "-15px"})}
    (.format published (java.time.format.DateTimeFormatter/ofPattern "MMMM dd y"))
    (when-let [tags (->> tags
                         (map (fn [tag]
                                [:a {:href (str "/fermentations/tag/" (name tag))
                                     :style (html/styles
                                             {:color "#999"})}
                                 (tag-name tag)]))
                         seq)]
      (concat [", Tags: "] (interpose ", " tags) [[:hr]]))]))

(defn render-page [{:keys [title body] :as page} & [prev next]]
  (fn [req]
    (let [html (format "<h1>%s</h1>%s%s" title (byline page) body)]
      (html/layout-page req html {:page-title title
                                  :prev prev
                                  :next next}))))

(defn prepare-pages [ferms]
  (->> (concat [nil] ferms)
       (partition-all 3 1)
       (remove #(nil? (second %)))
       (map (fn [[p f n]]
              [(:url f) (render-page f (or p {:url "/fermentations/"
                                              :title "Fermentations"}) n)]))
       (into {})))

(defn render-blurbs [ferms req]
  (apply vector
         :div
         (->> ferms
              (filter :published)
              (sort-by :published)
              (mapv (fn [{:keys [published url batch title]}]
                      [:div
                       [:div.vs-s
                        [:h3 [:a {:href url
                                  :style (html/styles
                                          {:display "flex"
                                           :justify-content "space-between"})}
                              batch
                              " "
                              title
                              [:span {:style (html/styles {:color "#ccc"})} published]]]
                        [:hr]]]))
              reverse)))

(defn prepare-tag-page [tag fermentations]
  [(str "/fermentations/tag/" (name tag) "/")
   (fn [req]
     (let [tag-name (tag-name tag)]
       (html/layout-page req (format "<h1>%s</h1>" tag-name)
                         {:page-title (str tag-name " fermentations - Christian Johansen")
                          :page-fn (partial render-blurbs (filter #(contains? (:tags %) tag) fermentations))})))])
