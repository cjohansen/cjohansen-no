(ns cjohansen-no.recipe
  (:require [cjohansen-no.html :as html]
            [cjohansen-no.tech-blog :as tech-blog]
            [ui.elements :as e]
            [datomic.api :as d]
            [cjohansen-no.markdown :as md]))

(defn recipe-page [post & [now]]
  {:open-graph/title (:recipe/title post)
   :open-graph/image (:recipe/image post)
   :page-title (:recipe/title post)
   :body [:div
          (e/simple-header)
          (->> (:recipe/sections post)
               (sort-by :section/number)
               (map tech-blog/post-section))
          (e/footer)]})

(defn render-page [req post]
  (html/layout-page-new req (recipe-page post)))

(defn recipes [db]
  (->> (d/q '[:find ?e ?t
              :in $
              :where
              [?e :browsable/kind :page/recipe-post]
              [?e :recipe/title ?t]]
            db)
       (sort-by second)
       (map #(d/entity db (first %)))))

(defn teaser [{:recipe/keys [title description tags] :browsable/keys [url]}]
  {:title title
   :url url
   :kind :article
   :tags (->> tags
              (sort-by :tag/name)
              (map (fn [tag] {:title (:tag/name tag)
                              :url (:browsable/url tag)})))})

(defn render-listing [req page]
  (html/layout-page-new
   req
   {:open-graph/title (:page/title page)
    :page-title (:page/title page)
    :body [:div
           (e/simple-header)
           (e/section {:content (md/to-html (:page/body page))})
           (e/teaser-section
            {:title "Oppskrifter"
             :teasers (map teaser (recipes (d/entity-db page)))})
           (e/footer)]}))
