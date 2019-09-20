(ns cjohansen-no.tech-blog
  (:require [cjohansen-no.html :as html]
            [datomic.api :as d]
            [me.raynes.cegdown :as md]
            [ui.elements :as e]))

(defn load-posts [db]
  (->> (d/q '[:find ?e
              :in $
              :where
              [?e :tech-blog/published]]
            db)
       (map #(d/entity db (first %)))))

(defmulti post-section (fn [section] (or (:section/type section) :section)))

(defmethod post-section :default [{:section/keys [body title sub-title theme type]}]
  (prn sub-title)
  (let [el (e/section
               {:title title
                :sub-title sub-title
                :content (md/to-html body html/pegdown-options)
                :class (some-> type name)})]
    (if theme
      [:div {:className (str "theme-" (name theme))} el]
      el)))

(defn tech-blog-page [post]
  {:open-graph/title (or (:tech-blog/short-title post)
                         (:tech-blog/title post))
   :open-graph/image (:tech-blog/image post)
   :page-title (:tech-blog/title post)
   :body [:div
          (e/header)
          (map post-section (->> (:tech-blog/sections post)
                                      (sort-by :section/number)))]})

(defn render-page [post]
  (fn [req]
    (html/layout-page-new req (tech-blog-page post))))

(comment
  (def conn (d/connect "datomic:mem://blog"))
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
