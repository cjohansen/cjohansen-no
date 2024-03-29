(ns cjohansen-no.tech-blog
  (:require [cjohansen-no.html :as html]
            [cjohansen-no.markdown :as md]
            [datomic.api :as d]
            [ui.elements :as e])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)))

(defmulti post-section (fn [section] (or (:section/type section) :section)))

(defmethod post-section :default [{:section/keys [body title sub-title theme type] :as opt}]
  (e/section
      {:title title
       :sub-title sub-title
       :heading-level 2
       :meta (when (or (:published opt) (:updated opt))
               {:published (:published opt) :updated (:updated opt)})
       :content (md/to-html body)
       :theme theme
       :type type}))

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
   :open-graph/description (or (:tech-blog/short-description post)
                               (:tech-blog/description post))
   :open-graph/image (:tech-blog/image post)
   :page-title (:tech-blog/title post)
   :body [:div
          (e/simple-header)
          (->> (:tech-blog/sections post)
               (sort-by :section/number)
               (add-byline (or now (java.time.Instant/now)) post)
               (map post-section))
          (e/footer)]})

(defn teaser [{:tech-blog/keys [short-title title description published tags] :browsable/keys [url]}]
  {:title (or short-title title)
   :published (ymd (->ldt published))
   :url url
   :description (md/to-html description)
   :kind :article
   :tags (->> tags
              (sort-by :tag/name)
              (map (fn [tag] {:title (:tag/name tag)
                              :url (:browsable/url tag)})))})

(defn tag-page [req tag]
  (html/layout-page-new
   req
   {:open-graph/title (:tag/name tag)
    :page-title (:tag/name tag)
    :body [:div
           (e/simple-header)
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

(defn blog-posts [db]
  (->> (d/q '[:find ?e ?p
              :in $
              :where
              [?e :browsable/kind :page/tech-post]
              [?e :tech-blog/published ?p]]
            db)
       (sort-by second)
       reverse
       (map #(d/entity db (first %)))))

(defn frontpage [req page]
  (html/layout-page-new
   req
   {:open-graph/title (:frontpage/title page)
    :page-title (:frontpage/title page)
    :body [:div
           (e/header)
           (e/section {:content (md/to-html (:frontpage/description page))})
           (e/teaser-section
            {:title "Blog posts"
             :teasers (map teaser (blog-posts (d/entity-db page)))})
           (e/footer)]}))

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
