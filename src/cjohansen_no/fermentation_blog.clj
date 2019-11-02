(ns cjohansen-no.fermentation-blog
  (:require [cjohansen-no.html :as html]
            [clojure.string :as str]
            [datomic.api :as d]
            [me.raynes.cegdown :as md]
            [ui.elements :as e])
  (:import java.time.format.DateTimeFormatter
           java.time.LocalDateTime))

(defn bread-page [post & [now]]
  {:open-graph/title (:bread/title post)
   :open-graph/image (:bread/image post)
   :page-title (:bread/title post)
   :body [:div
          (e/header)
          (e/footer)]})

(defn render-page [req post]
  (html/layout-page-new req (bread-page post)))
