(ns cjohansen-no.tech-blog
  (:require [cjohansen-no.html :as html]
            [clojure.string :as str]
            [datomic.api :as d]
            [me.raynes.cegdown :as md]
            [ui.elements :as e])
  (:import java.time.format.DateTimeFormatter
           java.time.LocalDateTime))

(defn load-posts [db]
  (->> (d/q '[:find ?e
              :in $
              :where
              [?e :tech-blog/published]]
            db)
       (map #(d/entity db (first %)))))

(defn load-tags [db]
  (->> (d/q '[:find ?e
              :in $
              :where
              [?e :tag/name]]
            db)
       (map #(d/entity db (first %)))))

(defn find-by-url [db url]
  (->> (d/q '[:find ?e .
              :in $ ?url
              :where
              [?e :browsable/url ?url]]
            db url)
       (d/entity db)))

(defn tag-url [tag]
  (str "/" (str/replace (str/lower-case (:tag/name tag)) #"[^a-z0-9]+" "-") "/"))

(defmulti post-section (fn [section] (or (:section/type section) :section)))

(defmethod post-section :default [{:section/keys [body title sub-title theme type] :as opt}]
  (let [el (e/section
               {:title title
                :sub-title sub-title
                :meta (when (or (:published opt) (:updated opt))
                        {:published (:published opt) :updated (:updated opt)})
                :content (md/to-html body html/pegdown-options)
                :class (str (some-> type name)
                            (when theme
                              (str " theme-" (name theme))))})]
    el))

(defn ->ldt [inst]
  (when inst
    (LocalDateTime/ofInstant (.toInstant inst) (java.time.ZoneId/of "Europe/Oslo"))))

(defn ymd [^LocalDateTime ldt]
  (.format ldt (DateTimeFormatter/ofPattern "MMMM d yyy")))

(defn md [^LocalDateTime ldt]
  (.format ldt (DateTimeFormatter/ofPattern "MMMM d")))

(defn add-byline [now {:tech-blog/keys [published updated]} sections]
  (let [published (->ldt published)
        updated (->ldt updated)
        sections (into [] sections)
        ks (cond
             (and published updated)
             {:updated (ymd updated)
              :published (ymd published)}
             published {:published (ymd published)})]
    (into [(merge (into {} (first sections)) ks)]
          (rest sections))))

(defn tech-blog-page [post & [now]]
  {:open-graph/title (or (:tech-blog/short-title post)
                         (:tech-blog/title post))
   :open-graph/image (:tech-blog/image post)
   :page-title (:tech-blog/title post)
   :body [:div
          (e/header)
          (->> (:tech-blog/sections post)
               (sort-by :section/number)
               (add-byline (or now (java.time.Instant/now)) post)
               (map post-section))
          (e/footer)]})

(defn teaser [{:tech-blog/keys [short-title title published] :browsable/keys [url]}]
  {:title (or short-title title)
   :published (ymd (->ldt published))
   :url url})

(defn tag-page [req tag]
  (html/layout-page-new
   req
   {:open-graph/title (:tag/name tag)
    :page-title (:tag/name tag)
    :body [:div
           (e/header)
           (e/teaser-section
            {:title (:tag/name tag)
             :teasers (->> tag
                           :tech-blog/_tags
                           (filter :tech-blog/published)
                           (sort-by :tech-blog/published)
                           reverse
                           (map teaser))})
           (e/footer)]}))

(defn render-page [req post]
  (html/layout-page-new req (tech-blog-page post)))

(comment
  (def conn (d/connect "datomic:mem://blog"))

  (load-posts (d/db conn))

  (tag-url {:tag/name "tools.deps"})

  (let [db (d/db conn)]
    (->> (d/q '[:find ?e
                :in $
                :where
                [?e :tech-blog/published]]
              db)
         (map #(d/entity db (first %)))
         first
         tech-blog-page))
)
