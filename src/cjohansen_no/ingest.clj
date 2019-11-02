(ns cjohansen-no.ingest
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [datomic.api :as d]
            [java-time-literals.core :as time]
            [mapdown.core :as md]
            [stasis.core :as stasis])
  (:import [java.time LocalDateTime ZoneId]))

::time/keep

(defn ->image-map [image]
  (cond
    (string? image) {:image/url image}
    (nil? image) nil
    :default
    (->> image
         (map (fn [[k v]] [(keyword "image" (name k)) v]))
         (into {}))))

(defn ->inst [^LocalDateTime local-date-time]
  (-> local-date-time
      (.atZone (ZoneId/of "Europe/Oslo"))
      .toInstant
      java.util.Date/from))

(defn parse-val [v]
  (try
    (let [val (read-string v)]
      (cond
        ;; A symbol probably means we read the first word of an unquoted text.
        ;; Return the full text string instead.
        (symbol? val) v

        ;; A sentence starting with a number, e.g. "50% Whole wheat" will be
        ;; read as a number, try to detect this mistake
        (and (number? val) (not= (str/trim v) (str val)))
        v

        :default val))
    (catch Exception e
      v)))

(defn step-ingredient [data]
  (->> data
       (map (fn [[k v]]
              (let [k (if (= :type k) :ingredient k)]
                [(keyword "step-ingredient" (name k))
                 (cond
                   (= :ingredient k) [:ingredient/id v]
                   (= :amount k) (float v)
                   :default v)])))
       (into {})))

(defn refine-val
  "Imbues certain key/value pairs with site-specific meaning - e.g. local date
  times should be instants in Europe/Oslo."
  [k v]
  (if (#{:body :title} k)
    v
    (let [v (parse-val v)]
      (cond
        (instance? java.time.LocalDateTime v) (->inst v)
        (= :tags k) (map (fn [t] [:tag/id (keyword "tag" (name t))]) v)
        (= :image k) (->image-map v)
        (= :ingredients k) (map step-ingredient v)
        :default v))))

(defn parse-md-section [section]
  (->> section
       (map (fn [[k v]] [k (refine-val k v)]))
       (into {})))

(defn parse-mapdown-db-file
  ([file-name] (parse-mapdown-db-file file-name (slurp (io/resource file-name))))
  ([file-name content]
   (let [[_ url] (re-find #"^(.*)\.md$" file-name)
         sections (->> content md/parse (map parse-md-section))
         section? (comp #{:section} :type)
         max-sections (count (filter section? sections))]
     (loop [post {:sections []}
            [section & sections] sections]
       (if section
         (recur
          (match (:type section)
            :meta (let [locale (or (some-> (:locale section) keyword) :en/US)
                        meta-section (-> section
                                         (dissoc :type :locale)
                                         (assoc :url (str url "/"))
                                         (assoc :i18n/locale locale))]
                    (assoc post :meta meta-section))
            :section (let [id (- max-sections (count (filter section? sections)))]
                       (update post :sections conj (-> section
                                                       (assoc :number id)
                                                       (assoc :id (str url "#" id))))))
          sections)
         post)))))

(defn pick-keys [m ks]
  (set/rename-keys
   (->> m
        (filter (fn [[k v]] (contains? ks k)))
        (into {}))
   ks))

(def bread-keys
  {:title :bread/title
   :description :bread/description
   :image :bread/image
   :published :bread/published
   :updated :bread/updated
   :sections :bread/sections})

(def blog-post-keys
  {:title :tech-blog/title
   :short-title :tech-blog/short-title
   :description :tech-blog/description
   :image :tech-blog/image
   :published :tech-blog/published
   :updated :tech-blog/updated
   :tags :tech-blog/tags
   :sections :tech-blog/sections})

(def section-keys
  {:id :section/id
   :number :section/number
   :body :section/body
   :title :section/title
   :sub-title :section/sub-title
   :image :section/image
   :section-type :section/type
   :theme :section/theme
   :ingredients :section/ingredients
   :time :section/time})

(defn blog-post [kind url {:keys [meta sections]} ks]
  (-> (pick-keys meta ks)
      (assoc :browsable/url url)
      (assoc :browsable/kind kind)
      (assoc :db/id url)
      (assoc (:sections ks) (map #(pick-keys % section-keys) sections))))

(defn tech-blog-post [parsed]
  (blog-post :page/tech-post (str/replace (-> parsed :meta :url) #"^tech" "") parsed blog-post-keys))

(defn bread-blog-post [parsed]
  (blog-post :page/bread-post (str "/" (-> parsed :meta :url)) parsed bread-keys))

(defn tag-tx-data [content]
  (->> content
       read-string
       (map (fn [[k v]] {:tag/id k,
                         :tag/name v
                         :browsable/url (str "/" (str/replace (str/lower-case v) #"[^a-z0-9]+" "-") "/")
                         :browsable/kind :page/tech-tag}))))

(defn unique-attrs [db]
  (->> (d/q '[:find ?a
              :in $
              :where
              [?a :db/unique :db.unique/identity]]
            db)
       (map first)
       set))

(defn resource-retractions [db resource]
  (let [attrs (unique-attrs db)]
    (->> (d/q '[:find ?e ?a ?v
                :in $ [?attr ...] ?f
                :where
                [?e ?attr _ ?t]
                [?t :tx/source-file ?f]
                [?e ?a ?v ?t]]
              db
              attrs
              (.getPath resource))
         (remove (comp attrs second)))))

(defn resource-ingested-at [db resource]
  (d/q '[:find ?txi .
         :in $ ?f
         :where
         [?attr :db/unique :db.unique/identity]
         [?e ?attr]
         [?e _ _ ?t]
         [?t :tx/source-file ?f]
         [?t :db/txInstant ?txi]]
       db
       (.getPath resource)))

(defn file-tx
  ([db file-name]
   (file-tx file-name identity))
  ([db file-name f]
   (let [resource (io/resource file-name)]
     (file-tx db resource (slurp resource) f)))
  ([db resource content f]
   (let [ingested-at (resource-ingested-at db resource)]
     (if (and ingested-at
              (<= (.lastModified (io/file (.getFile resource))) (.getTime ingested-at)))
       []
       (->>
        [(map (fn [[e a v]] [:db/retract e a v]) (resource-retractions db resource))
         (concat
          [[:db/add "datomic.tx" :tx/source-file (.getPath resource)]]
          (f content))]
        (filter (comp seq first)))))))

(defn slurp-posts [db dir tx-data-f]
  (->> (stasis/slurp-directory (io/resource dir) #"\.md$")
       (mapcat
        (fn [[file-name content]]
          (let [path (str dir file-name)]
            (file-tx
             db
             (io/resource path)
             (parse-mapdown-db-file path content)
             (comp vector tx-data-f)))))))

(defn db-conn []
  (d/create-database "datomic:mem://blog")
  (let [conn (d/connect "datomic:mem://blog")]
    (d/transact conn (read-string (slurp (io/resource "schema.edn"))))
    conn))

(defn ingest-everything [db]
  (concat
   (file-tx db "tags.edn" tag-tx-data)
   (file-tx db "ingredients.edn" read-string)
   (slurp-posts db "tech" tech-blog-post)
   (slurp-posts db "fermentations" bread-blog-post)))

(comment
  (d/delete-database "datomic:mem://blog")
  (d/create-database "datomic:mem://blog")
  (def conn (d/connect "datomic:mem://blog"))
  (d/transact conn (read-string (slurp (io/resource "schema.edn"))))

  (doseq [tx-data (ingest-everything (d/db conn))]
    @(d/transact conn tx-data))

  (doseq [tx-data (file-tx (d/db conn) "tags.edn" tag-tx-data)]
    @(d/transact conn tx-data))

  (file-tx (d/db conn) "tags.edn" tag-tx-data)

  (into {} (d/entity (d/db conn) 17592186045424)) ;; EFS

  (->> (d/q '[:find ?e ?title
              :in $
              :where
              [?e :tech-blog/title ?title]]
            (d/db conn))
       (into {}))

  )
