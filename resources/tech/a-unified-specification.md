--------------------------------------------------------------------------------
:type :meta
:title A Unified Specification
:published #time/ldt "2018-09-26T12:00"
:tags [:clojure :datascript :spec]
:description

An approach to describing the structure of data in ClojureScript applications in
one place, and using it to power Datascript schemas, specs, and coercions for
data from external sources.

--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title A Unified Specification
:body

Your frontend application state lives in
[Datascript](https://github.com/tonsky/datascript) (it should anyway), and you
use [spec](https://clojure.org/about/spec) (again, you should) to enforce
constraints on your data. Datascript needs a schema that asserts _some_ facts
about your data, while specs cover the rest. If you're consuming an API not
specifically tailored for your app you also need some code to map API data to
the Datascript schema.

Problem: the structure of your data is now scattered across three distinct
pieces of code.


--------------------------------------------------------------------------------
:type :section
:body

To visualize the problem, we'll consume some data from
[The Studio Ghibli API](https://ghibliapi.herokuapp.com), specifically, data
about a movie:

```sh
curl https://ghibliapi.herokuapp.com/films/58611129-2dbc-4a81-a72f-77ddfc1b1b49
```

```json
{
  "id": "58611129-2dbc-4a81-a72f-77ddfc1b1b49",
  "title": "My Neighbor Totoro",
  "description": "Two sisters move to the country with their father ...",
  "director": "Hayao Miyazaki",
  "producer": "Hayao Miyazaki",
  "release_date": "1988",
  "rt_score": "93",
  "people": [
    "https://ghibliapi.herokuapp.com/people/986faac6-67e3-4fb8-a9ee-bad077c2e7fe",
    "https://ghibliapi.herokuapp.com/people/d5df3c04-f355-4038-833c-83bd3502b6b9",
    "https://ghibliapi.herokuapp.com/people/3031caa8-eb1a-41c6-ab93-dd091b541e11",
    "https://ghibliapi.herokuapp.com/people/87b68b97-3774-495b-bf80-495a5f3e672d",
    "https://ghibliapi.herokuapp.com/people/d39deecb-2bd0-4770-8b45-485f26e1381f",
    "https://ghibliapi.herokuapp.com/people/f467e18e-3694-409f-bdb3-be891ade1106",
    "https://ghibliapi.herokuapp.com/people/08ffbce4-7f94-476a-95bc-76d3c3969c19",
    "https://ghibliapi.herokuapp.com/people/0f8ef701-b4c7-4f15-bd15-368c7fe38d0a"
  ],
  "species": [
    "https://ghibliapi.herokuapp.com/species/af3910a6-429f-4c74-9ad5-dfe1c4aa04f2",
    "https://ghibliapi.herokuapp.com/species/603428ba-8a86-4b0b-a9f1-65df6abef3d3",
    "https://ghibliapi.herokuapp.com/species/74b7f547-1577-4430-806c-c358c8b6bcf5"
  ],
  "locations": [
    "https://ghibliapi.herokuapp.com/locations/"
  ],
  "vehicles": [
    "https://ghibliapi.herokuapp.com/vehicles/"
  ],
  "url": "https://ghibliapi.herokuapp.com/films/58611129-2dbc-4a81-a72f-77ddfc1b1b49"
}
```

To avoid data fetching mechanics, we'll cut out most relations, and inline the
ones we care about modelling:

```json
{
  "id": "58611129-2dbc-4a81-a72f-77ddfc1b1b49",
  "title": "My Neighbor Totoro",
  "description": "Two sisters move to the country with their father ...",
  "release_date": "1988",
  "people": [
    {
      "id": "986faac6-67e3-4fb8-a9ee-bad077c2e7fe",
      "name": "Satsuki Kusakabe",
      "age": "11"
    },
    {
      "id": "d5df3c04-f355-4038-833c-83bd3502b6b9",
      "name": "Mei Kusakabe",
      "age": "4"
    }
  ]
}
```

--------------------------------------------------------------------------------
:type :section
:theme :light1
:title The Datascript schema
:body

Datascript schemas only need to specify attributes that should be unique, that
are references, or that are collections. It does not care about or enforce the
primitive type of attributes, and in fact it even allows storing attributes it
doesn't know about. So a schema for the above could look something like:

```clj
(def schema
  {:movie/id {:db/unique :db.unique/identity}
   :movie/people {:db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/many}
   :person/id {:db/unique :db.unique/identity}})
```

The rest of the attributes don't need any further description. The problem with
this approach is that the schema doesn't really tell you what the entire schema
looks like. To mitigate this, I've sometimes included empty placeholders for
documentation purposes, but that has problems of its own - who will remember to
keep those placeholders up to date as the schema changes?

--------------------------------------------------------------------------------
:type :section
:title The specs
:body

Next up, we'd like to write Clojure specs for the data - not primarily to
validate data from the API, but to aid in transformation of data, and for use
elsewhere in the codebase e.g. with `s/fdef`.

```clj
(defn numberify [s]
  (cond
    (number? s) s
    (re-matches #"^\d+$") (js/parseInt s 10)
    :default :cljs.spec.alpha/invalid))

(s/def ::number (s/conformer numberify))

(s/def :person/id (s/conformer uuid))
(s/def :person/name string?)
(s/def :person/age ::number)
(s/def :person/entity (s/keys :req [:person/id :person/name :person/age]))

(s/def :movie/id (s/conformer uuid))
(s/def :movie/title string?)
(s/def :movie/description string?)
(s/def :movie/release-date ::number)
(s/def :movie/people (s/coll-of :person/entity))
(s/def :movie/entity (s/keys :req [:movie/id :movie/title :movie/description
                                   :movie/release-date :movie/people]))
```

This description of our data is much more complete. In fact, the only thing not
encoded in this representation is the fact that the two `id` attributes are
unique.

--------------------------------------------------------------------------------
:type :section
:theme :light1
:title The mapping
:body

Finally, I present to you some code to map the API data into our chosen
representation for storing in Datascript.

```clj
(defn convert-person-data [{:keys [id name age]}]
  {:person/id (s/conform :person/id id)
   :person/name (s/conform :person/name name)
   :person/age (s/conform :person/age age)})

(defn convert-api-data [movie]
  {:movie/id (s/conform :movie/id (:id movie))
   :movie/title (s/conform :movie/title (:title movie))
   :movie/description (s/conform :movie/description (:description movie))
   :movie/release-date (s/conform :movie/release-date (:release_date movie))
   :movie/people (map convert-person-data (:people data))})
```

Relatively tedious, but thanks to conforming specs, not as bad as it could've
been.

As promised, information about the structure of our application data is
scattered among three individual sections of code that are hard to intermingle
in any sensible way. This particular schema is small, so it's not all that bad.
But take a schema of 5-6 entities with a bunch of attributes each, and you
quickly lose track of the full picture.

--------------------------------------------------------------------------------
:type :section
:tile Step 1: Inline those specs
:body


In an attempt to create a more singular description of our data, we will move
the specs *into* the Datascript schema. This won't actually work, but it'll
define a goal:

```clj
(def schema
  {:person/id {:db/unique :db.unique/identity
               :schema/spec (s/conformer uuid)}
   :person/name {:schema/spec string?}
   :person/age {:schema/spec ::number}
   :person/entity {:schema/spec (s/keys :req [:person/id :person/name :person/age])}

   :movie/id {:db/unique :db.unique/identity
              :schema/spec (s/conformer uuid)}
   :movie/title {:schema/spec string?}
   :movie/description {:schema/spec string?}
   :movie/release-date {:schema/spec ::number}
   :movie/people {:db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/many
                  :schema/spec (s/coll-of :person/entity)}
   :movie/entity {:schema/spec (s/keys :req [:movie/id :movie/title :movie/description
                                             :movie/release-date :movie/people])}})
```

`schema` is a custom namespace that I just introduced. We'll make use of it
shortly. Because the schema now has extraneous junk in it, Datascript will no
longer eat it raw. We'll need a function that turns this back into a pure
Datascript schema. Since we'll need a supporting function anyway, let's see if
we can make some more improvements while we're at it.

When we reviewed our original specs, we concluded that they described everything
about our data _except for uniqueness constraints_. It just so happens that spec
has APIs to work with defined specs as data, allowing us to extract e.g. keys
from a `(s/keys)` spec and more. That means we no longer need `:db/valueType` or
`:db/cardinality` - the former can be induced from `(s/keys)` specs (those will
be references) and the latter from `(s/coll-of)` specs (collection -
`:db.cardinality/many`).

This leaves us with this leaner representation:

```clj
(def schema
  {:person/id {:db/unique :db.unique/identity
               :schema/spec (s/conformer uuid)}
   :person/name {:schema/spec string?}
   :person/age {:schema/spec ::number}
   :person/entity {:schema/spec (s/keys :req [:person/id :person/name :person/age])}

   :movie/id {:db/unique :db.unique/identity
              :schema/spec (s/conformer uuid)}
   :movie/title {:schema/spec string?}
   :movie/description {:schema/spec string?}
   :movie/release-date {:schema/spec ::number}
   :movie/people {:schema/spec (s/coll-of :person/entity)}
   :movie/entity {:schema/spec (s/keys :req [:movie/id :movie/title :movie/description
                                             :movie/release-date :movie/people])}})
```

## Step 2: Define specs

Before we can extract the Datascript schema, we'll need to define the specs so
we can mine them for data. `cljs.spec.alpha/def` is a macro, and in order to
call it correctly on behalf of the schema definition, we need a macro to define
the schema as well.

The macro goes into `unified-schema/macros.cljc`:

```clj
(ns unified-schema.macros
  #?(:cljs (:require [cljs.spec.alpha])))

(defmacro defschema [name schema]
  (apply list 'do (concat
                   (for [[attr attr-def] schema]
                     `(cljs.spec.alpha/def ~attr ~(:schema/spec attr-def)))
                   `[(def ~name ~schema)])))
```

...and can be used like so:

```clj
(ns unified-schema.example
  (:require [unified-schema.macros :refer-macros [defschema]])

(defschema example-schema
  {:person/id {:db/unique :db.unique/identity
               :schema/spec (s/conformer uuid)}
   :person/name {:schema/spec string?}
   :person/age {:schema/spec ::number}
   :person/entity {:schema/spec (s/keys :req [:person/id :person/name :person/age])}

   :movie/id {:db/unique :db.unique/identity
              :schema/spec (s/conformer uuid)}
   :movie/title {:schema/spec string?}
   :movie/description {:schema/spec string?}
   :movie/release-date {:schema/spec ::number}
   :movie/people {:schema/spec (s/coll-of :person/entity)}
   :movie/entity {:schema/spec (s/keys :req [:movie/id :movie/title :movie/description
                                             :movie/release-date :movie/people])}})
```

Now the specs will be defined, and `example-schema` will refer to our schema
data.

## Step 3: Extract Datascript schema

To extract the schema, we'll start by simply preserving all the keys in the `db`
namespace:

```clj
(defn select-namespaced-keys [m ns]
  (->> (keys m)
       (filter #(= (namespace %) ns))
       (select-keys m)))

(defn extract-schema [attributes]
  (->> attributes
       (map (fn [[k v]] [k (select-namespaced-keys v "db"]))
       (into {})))
```

We will now add `:db.cardinality/many` to any attribute that has a `s/coll-of`
spec, and `:db.type/ref` to any attribute that has a `s/keys` spec. You can
inspect the underlying data structure of a spec with `s/form`:

```clj
(s/form :person/entity)
;;=> (clojure.spec.alpha/keys :req [:person/id :person/name :person/age])
```

To devise a generalized solution, we'd also like to support this spec:

```clj
(s/def :person/entity
  (s/and (s/keys :req [:user/name])
         (s/or :email (s/keys :req [:user/email])
               :phone (s/keys :req [:user/tel]))))

(s/form :person/entity)
;;(clojure.spec.alpha/and
;; (clojure.spec.alpha/keys :req [:user/name])
;; (clojure.spec.alpha/or
;;  :email
;;  (clojure.spec.alpha/keys :req [:user/email])
;;  :phone
;;  (clojure.spec.alpha/keys :req [:user/tel])))
```

To work with this data, I wrote two helper functions (see all the way to the
bottom if you're interested):

* `coll-of`, which returns the first `s/coll-of` spec
* `specced-keys`, which returns a set of all keys, required and optional, for a
  map spec (`#{:user/name :user/email :user/tel}` in the above example).

Using these two functions, we can add the cardinality and ref types to the
relevant attributes:

```clj
(defn- schema-attrs [attributes attr-key attr-def]
  (let [coll-type (coll-of attr-key)]
    (-> (select-namespaced-keys attributes "db")
        (assoc-non-nil :db/cardinality (when coll-type :db.cardinality/many))
        (assoc-non-nil :db/valueType (when (or (seq (specced-keys attr-key))
                                               (seq (specced-keys coll-type)))
                                       :db.type/ref)))))

(defn extract-schema [attributes]
  (->> attributes
       (map (fn [[k v]] [k (schema-attrs attributes k v]))
       (into {})))
```

Finally, we'll evict all attribute definitions that don't have any descriptors
in the `db` namespace:

```clj
(defn extract-schema [attributes]
  (let [schema (->> attributes
                    (map (fn [[k v]] [k (schema-attrs attributes k v)]))
                    (into {}))]
    (->> (keys attributes)
         (filter #(not (seq (select-namespaced-keys (% attributes) "db"))))
         (apply dissoc schema))))
```

## Step 4: Automate data transformations

We've unified the Datascript schema and specs. What about the mapping from API
data to schema data? It would be neat if we could achieve that declaratively as
well. In this particular case the mapping was quite straight forward: attributes
have different names, and we want to pass values through our specs. The
declarative bit of the solution could look like this:

```clj
(defschema example-schema
  {:person/id {:db/unique :db.unique/identity
               :schema/spec (s/conformer uuid)
               :schema/source :id}
   :person/name {:schema/spec string? :schema/source :name}
   :person/age {:schema/spec ::number :schema/source :age}
   :person/entity {:schema/spec (s/keys :req [:person/id :person/name :person/age])}

   :movie/id {:db/unique :db.unique/identity
              :schema/spec (s/conformer uuid)
              :schema/source :id}
   :movie/title {:schema/spec string? :schema/source :title}
   :movie/description {:schema/spec string? :schema/source :description}
   :movie/release-date {:schema/spec ::number :schema/source :release_date}
   :movie/people {:schema/spec (s/coll-of :person/entity) :schema/source :people}
   :movie/entity {:schema/spec (s/keys :req [:movie/id :movie/title :movie/description
                                             :movie/release-date :movie/people])}})
```

Frequently, your schema will contain namespaced versions of API data keys (e.g.
`:person/name` vs `"name"`). Because this particular mapping is so common, we'll
just bolt it into the converter, and can leave them out of our schema.

The implementation of `convert-data` looks like this:

```clj
(defn convert-data [attributes api-data key]
  (let [{:keys [schema/source]} (attributes key)
        data (if source
               (get api-data source)                   ;; Pick the specified source key
               (get api-data key                       ;; ...try the verbatim key
                    (get api-data (keyword (name key)) ;; ...or the unqualified key
                         api-data)))                   ;; ...or just use the raw data
        collection-type (coll-of key)
        keys (specced-keys key)]
    (cond
      ;; For keys spec, build the value by recursively converting each key into a map
      (seq keys) (->> keys
                      (map (fn [k] [k (convert-data attributes api-data k)]))
                      (into {}))

      ;; For collections, recursively convert raw data elements
      collection-type (map #(convert-data attributes % collection-type) api-data)

      ;; Found a leaf value - conform it, or assert to fail
      :default (if (s/valid? key api-data)
                 (s/conform key api-data)
                 (s/assert key api-data)))))
```

There is one more feature that could be useful in this utility: the ability to
provide a custom mapper for a value. We do already have this, through the key
specs. However, sometimes we need to break down data in ways that are unsuitable
for conformers to handle, such as converting `"SomeLabel_23"` into
`{:thing/label "SomeLabel" :thing/id 23}` before continuing processing. This
could be fixed with a `:schema/mapper` function that receives the current data
and produces the data to process.

--------------------------------------------------------------------------------
:type :section
:theme :dark1
:title In Summary
:body

With just a little bit of abstraction on top of Datascript schemas and specs,
we're able to produce a Datascript schema, specs, and API->schema mapping from
one and the same declarative piece of code:

```clj
(defn numberify [s]
  (cond
    (number? s) s
    (re-matches #"^\d+$") (js/parseInt s 10)
    :default :cljs.spec.alpha/invalid))

(s/def ::number (s/conformer numberify))

(defschema example-schema
  {:person/id {:db/unique :db.unique/identity
               :schema/spec (s/conformer uuid)}
   :person/name {:schema/spec string?}
   :person/age {:schema/spec ::number}
   :person/entity {:schema/spec (s/keys :req [:person/id :person/name :person/age])}

   :movie/id {:db/unique :db.unique/identity
              :schema/spec (s/conformer uuid)}
   :movie/title {:schema/spec string?}
   :movie/description {:schema/spec string?}
   :movie/release-date {:schema/spec ::number :schema/source :release_date}
   :movie/people {:schema/spec (s/coll-of :person/entity)}
   :movie/entity {:schema/spec (s/keys :req [:movie/id :movie/title :movie/description
                                             :movie/release-date :movie/people])}})

(extract-schema example-schema) ;; Datascript schema
(convert-data example-schema api-data :movie/entity) ;; Transactionable data
```

This can be used with a wide variety of API->Datascript mappings, and helps keep
your schema definitions in one place. All thanks to the underlying data driven
abstractions of Datascript and clojure.spec. Data: you can't beat it.
