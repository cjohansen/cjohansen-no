(ns cjohansen-no.ingest
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [mapdown.core :as md])
  (:import java.time.LocalDateTime
           java.time.ZoneId))

(defn db-conn []
  (d/create-database "datomic:mem://blog")
  (let [conn (d/connect "datomic:mem://blog")]
    (d/transact conn (read-string (slurp (io/resource "schema.edn"))))
    conn))

(defn prune-nils [m]
  (->> m
       (remove (fn [[k v]] (nil? v)))
       (into {})))

(defn parse-section [id {:keys [body section-type theme title sub-title]}]
  (cond-> {:section/body body
           :section/number id}
    section-type (assoc :section/type (read-string section-type))
    theme (assoc :section/theme (read-string theme))
    title (assoc :section/title title)
    sub-title (assoc :section/sub-title sub-title)))

(defn parse-image [image]
  (let [image (try
                (read-string image)
                (catch Exception e
                  image))]
    (if (string? image)
      {:image/url image}
      (->> image
           (map (fn [[k v]] [(keyword "image" (name k)) v]))
           (into {})))))

(defn parse-tech-post
  ([file-name] (parse-tech-post file-name (slurp (io/resource file-name))))
  ([file-name content]
   (let [[_ url] (re-find #"^tech(.*)\.md$" file-name)
         sections (md/parse content)
         max-sections (count sections)]
     (loop [post {:tech-blog/sections []}
            [section & sections] sections]
       (if section
         (recur
          (match (read-string (:type section))
            :meta (-> post
                      (assoc :browsable/url (str url "/"))
                      (assoc :tech-blog/title (:title section))
                      (assoc :tech-blog/short-title (:short-title section))
                      (assoc :tech-blog/image (parse-image (:image section)))
                      (assoc :i18n/locale (or (some-> (:locale section) keyword) :en/US))
                      (assoc :tech-blog/published (LocalDateTime/parse (:published section)))
                      (assoc :tech-blog/tags (->> (:tags section)
                                                  read-string
                                                  (map #(keyword "tag" (name %)))))
                      prune-nils)

            :section (let [id (- max-sections (count sections))]
                       (update post :tech-blog/sections conj (parse-section id section))))
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

  (d/q '[:find ?e
         :in $
         :where
         [?e :tech-blog/published]]
       (d/db conn))

  (let [db (d/db conn)]
    (->> (d/q '[:find ?e
                :in $
                :where
                [?e :tech-blog/published]]
              db)
         (map #(d/entity db (first %)))
         (map :tech-blog/title)))

  (d/transact conn [{:db/id "/some/path" :tech-blog/title nil}])

  (parse-tech-post "tech/clojure-in-production-tools-deps.md")
)
