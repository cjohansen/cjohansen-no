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
        (= :tags k) (map #(keyword "tag" (name %)) v)
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

(defn blog-post [url {:keys [meta sections]} ks]
  (-> (pick-keys meta ks)
      (assoc :browsable/url url)
      (assoc :db/id url)
      (assoc (:sections ks) (map #(pick-keys % section-keys) sections))))

(defn tech-blog-post [parsed]
  (blog-post (str/replace (-> parsed :meta :url) #"^tech" "") parsed blog-post-keys))

(defn bread-blog-post [parsed]
  (blog-post (str "/" (-> parsed :meta :url)) parsed bread-keys))

(defn tech-post-txes
  ([file-name] (tech-post-txes file-name (slurp (io/resource file-name))))
  ([file-name content] [(tech-blog-post (parse-mapdown-db-file file-name content))]))

(defn bread-post-txes
  ([file-name] (bread-post-txes file-name (slurp (io/resource file-name))))
  ([file-name content] [(bread-blog-post (parse-mapdown-db-file file-name content))]))

(defn tag-txes [content]
  (map (fn [[k v]] {:db/ident k, :tag/name v}) content))

(defn slurp-posts [dir txes-f]
  (->> (stasis/slurp-directory (io/resource dir) #"\.md$")
       (mapcat (fn [[file-name content]] (txes-f (str dir file-name) content)))))

(defn db-conn []
  (d/create-database "datomic:mem://blog")
  (let [conn (d/connect "datomic:mem://blog")]
    (d/transact conn (read-string (slurp (io/resource "schema.edn"))))
    conn))

(defn ingest-everything []
  [(tag-txes (read-string (slurp (io/resource "tags.edn"))))
   (read-string (slurp (io/resource "ingredients.edn")))
   (slurp-posts "tech" tech-post-txes)
   (slurp-posts "fermentations" bread-post-txes)])

(comment
  (d/delete-database "datomic:mem://blog")
  (d/create-database "datomic:mem://blog")
  (def conn (d/connect "datomic:mem://blog"))
  (d/transact conn (read-string (slurp (io/resource "schema.edn"))))

  (->> (stasis/slurp-directory (io/resource "tech") #"\.md$")
       (mapcat (fn [[file-name content]] (tech-post-txes (str "tech" file-name) content))))

  (d/transact conn (tag-txes (read-string (slurp (io/resource "tags.edn")))))
  (d/transact conn (read-string (slurp (io/resource "ingredients.edn"))))
  (d/transact conn (tech-post-txes "tech/clojure-in-production-tools-deps.md"))
  (d/transact conn (tech-post-txes "tech/a-better-playlist-shuffle-with-golang.md"))
  (d/transact conn (tech-post-txes "tech/a-unified-specification.md"))
  (d/transact conn (tech-post-txes "tech/an-introduction-to-elisp.md"))
  (d/transact conn (tech-post-txes "tech/annotating-datomic-transactions.md"))
  (d/transact conn (tech-post-txes "tech/aws-apigw-proxy-cloudformation.md"))
  (d/transact conn (tech-post-txes "tech/aws-free-tier.md"))
  (d/transact conn (tech-post-txes "tech/building-static-sites-in-clojure-with-stasis.md"))
  (d/transact conn (tech-post-txes "tech/clojure-to-die-for.md"))
  (d/transact conn (tech-post-txes "tech/css-grid.md"))
  (d/transact conn (tech-post-txes "tech/git-subtree-multiple-dirs.md"))
  (d/transact conn (tech-post-txes "tech/idempotent-cloudformation-updates.md"))
  (d/transact conn (tech-post-txes "tech/letsencrypt-haproxy-ssl.md"))
  (d/transact conn (tech-post-txes "tech/optimized-optimus-asset-paths-clojurescript.md"))
  (d/transact conn (tech-post-txes "tech/processing-data-with-clojure-and-golang.md"))
  (d/transact conn (tech-post-txes "tech/querying-across-datomic-databases.md"))
  (d/transact conn (tech-post-txes "tech/referentially-transparent-crud.md"))
  (d/transact conn (tech-post-txes "tech/tools-deps-figwheel-main-devcards-emacs.md"))
  (d/transact conn (tech-post-txes "tech/webslides-syntax-highlighting.md"))
  (d/transact conn (bread-post-txes "fermentations/2019-09-21-whole-wheat-rolls.md"))


  (parse-mapdown-db-file "tech/clojure-in-production-tools-deps.md")
  (tech-blog-post (parse-mapdown-db-file "tech/clojure-in-production-tools-deps.md"))
  (parse-mapdown-db-file "fermentations/2019-09-21-whole-wheat-rolls.md")

  (second (md/parse (slurp (io/resource "fermentations/2019-09-21-whole-wheat-rolls.md"))))

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
         (mapcat :tech-blog/sections)
         (map :section/id)))

)
