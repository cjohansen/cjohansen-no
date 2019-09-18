(ns cjohansen-no.ingest
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [mapdown.core :as md])
  (:import java.time.LocalDateTime
           java.time.ZoneId))

(defn parse-section [{:keys [body section-type theme]}]
  (cond-> {:section/body body}
    section-type (assoc :section/type (read-string section-type))
    theme (assoc :section/theme (read-string theme))))

(defn parse-tech-post
  ([file-name] (parse-tech-post file-name (slurp (io/resource file-name))))
  ([file-name content]
   (let [[_ url] (re-find #"^tech(.*)\.md$" file-name)
         sections (md/parse content)]
     (loop [post {:tech-blog/sections []}
            [section & sections] sections]
       (if section
         (recur
          (match (read-string (:type section))
            :meta (-> post
                      (assoc :browsable/url (str url "/"))
                      (assoc :tech-blog/title (:title section))
                      (assoc :tech-blog/published (LocalDateTime/parse (:published section)))
                      (assoc :tech-blog/tags (->> (:tags section)
                                                  read-string
                                                  (map #(keyword "tag" (name %))))))

            :section (update post :tech-blog/sections conj (parse-section section)))
          sections)
         post)))))

(defn ->inst [^LocalDateTime local-date-time]
  (java.util.Date/from (.toInstant (.atZone local-date-time (ZoneId/of "Europe/Oslo")))))

(defn tech-post-txes
  ([file-name] (tech-post-txes file-name (slurp (io/resource file-name))))
  ([file-name content]
   (let [post (parse-tech-post file-name content)]
     [(cond-> post
        (:tech-blog/published post) (update :tech-blog/published ->inst)
        :always (assoc :db/id (:tech-blog/url post)))])))

(defn tag-txes [content]
  (mapcat (fn [[k v]] [{:db/ident k, :tag/name v}]) content))

(comment
  (d/delete-database "datomic:mem://blog")
  (d/create-database "datomic:mem://blog")
  (def conn (d/connect "datomic:mem://blog"))
  (d/transact conn (read-string (slurp (io/resource "schema.edn"))))

  (d/transact conn (tag-txes (read-string (slurp (io/resource "tags.edn")))))
  (d/transact conn (tech-post-txes "tech/clojure-in-production-tools-deps.md"))

  (d/q '[:find ?title ?url
         :in $
         :where
         [?e :browsable/url ?url]
         [?e :tech-blog/title ?title]]
       (d/db conn))

  (parse-tech-post "tech/clojure-in-production-tools-deps.md")
)
