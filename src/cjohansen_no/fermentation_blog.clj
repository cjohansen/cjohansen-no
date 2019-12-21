(ns cjohansen-no.fermentation-blog
  (:require [cjohansen-no.html :as html]
            [cjohansen-no.time :as time]
            [cjohansen-no.bread :as bread]
            [cjohansen-no.markdown :as md]
            [clojure.string :as str]
            [datomic.api :as d]
            [ui.elements :as e])
  (:import java.time.format.DateTimeFormatter
           java.time.LocalDateTime))

(defn format-number [n]
  (let [int-equal? (= (int (* 10 n)) (* 10 (int n)))]
    (cond
      (and (<= 1 n) int-equal?) (str (int n))
      int-equal? (format "%.2f" n)
      :default (format "%.1f" n))))

(defn ingredient [{:keys [ingredient url amount bakers-ratio]}]
  {:amount (str (format-number amount) "g")
   :percent (str (format-number (* 100 bakers-ratio)) "&nbsp;%")
   :title (if url
            [:a {:href url} ingredient]
            ingredient)})

(defmulti post-section (fn [post section] (or (:section/type section) :section)))

(defn format-time [min]
  (cond
    (< min 60) (str min " minutes")
    (= 0 (mod min 60)) (str (/ min 60) " hours")
    (< min 120) (str (int (/ min 60)) " hour, " (mod min 60) " minutes")
    :else (str (int (/ min 60)) " hours, " (mod min 60) " minutes")))

(defn section-props [bread {:section/keys [body time ingredients title sub-title theme type] :as params}]
  (let [content (some-> body md/to-html)]
    {:heading-level 2
     :title title
     :sub-title sub-title
     :theme theme
     :type type
     :note (when time [:strong (format-time time)])
     :meta (when (or (:published params) (:updated params)) params)
     :content (list
               (when ingredients
                 (e/ingredient-list
                  (->> (bread/prepare-ingredients
                        (bread/step-ingredients ingredients)
                        (bread/bread-ingredients bread))
                       (map ingredient))))
               content)}))

(defmethod post-section :default [bread section]
  (e/section (section-props bread section)))

(defmethod post-section :media-back [bread section]
  (e/section-media-back
   (-> (section-props bread section)
       (assoc :media [:img.img {:src (:image/url (:section/image section))}]))))

(defn image-section [image]
  (when (:image/url image)
    (e/section-media
     {:media [:img.img {:src (str "/short-wide" (:image/url image)) :alt (:image/alt image)}]
      :size :mega-wide
      :theme :dark1})))

(defmethod post-section :image [bread section]
  (image-section (:section/image section)))

(defn byline [now {:bread/keys [published updated]}]
  (let [published (time/->ldt published)
        updated (time/->ldt updated)]
    (cond
      (and published updated)
      {:updated (time/ymd updated)
       :published (time/ymd published)}
      published {:published (time/ymd published)})))

(defn title-section [now {:bread/keys [title description] :as bread}]
  (e/section
   {:title title
    :theme :dark1
    :content (some-> description md/to-html)
    :meta (byline now bread)}))

(defn recipe-section [{:bread/keys [title image] :as bread}]
  (let [recipe (bread/recipe bread)]
    (e/section
      {:sub-title "Full recipe"
       :content
       (e/split
        {:front
         (e/ingredient-list
          (map ingredient (:ingredients recipe)))
         :back
         (e/kv-table
          [{:label "Total hydration", :val (str (int (* 100 (:hydration recipe))) "&nbsp;%")}
           {:label "Total time", :val (:time recipe)}])})
       :theme :dark1})))

(defn previous-bakes [bread]
  (let [db (d/entity-db bread)]
    (->> (d/q '[:find ?e
                :in $ ?published
                :where
                [?e :bread/published ?p]
                [?e :bread/image]
                [(.before ?p ?published)]]
              db (:bread/published bread))
         (map #(d/entity db (first %)))
         (sort-by :bread/published)
         reverse)))

(defn prepare-teaser [{:bread/keys [title published image] :browsable/keys [url]}]
  {:title title
   :url url
   :published (-> published time/->ldt time/ymd)
   :media (when image [:img.img {:src (str "/thumb" (:image/url image))}])})

(defn bread-page [post & [now]]
  {:open-graph/title (:bread/title post)
   :open-graph/image (:bread/image post)
   :page-title (:bread/title post)
   :body [:div
          (e/header)
          (title-section (or now (java.time.Instant/now)) post)
          (image-section (:bread/image post))
          (recipe-section post)
          (->> (:bread/sections post)
               (sort-by :section/number)
               (map #(post-section post %)))
          (e/teaser-section {:title "Previous bakes"
                             :teasers (->> (take 3 (previous-bakes post))
                                           (map prepare-teaser))})
          (e/footer)]})

(defn render-page [req post]
  (html/layout-page-new req (bread-page post)))

(comment
  (def conn (d/connect "datomic:mem://blog"))
  (def db (d/db conn))

  (->> (d/q '[:find ?e .
              :in $ ?url
              :where
              [?e :browsable/url ?url]]
            db "/fermentations/2019-09-21-whole-wheat-rolls/")
       (d/entity db)
       bread/recipe)
  )
