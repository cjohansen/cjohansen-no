# Annotating Datomic transactions

When [Datomic](http://www.datomic.com/) processes a transaction, it creates a
transaction entity and stores it along your other entities. The transaction has
one built-in attribute, `:db/txInstant`, which tells you when Datomic processed
the transaction. The transaction entity tells you which facts where inserted at
the same time, and when it happened. If that isn't enough, you can freely extend
this model.

## Back-dating initial import

Transaction ids are handled by Datomic. Transaction timestamps are also handled
by Datomic, and will reflect the clock by default. however, can be
altered by the application. If you opt to do this, there is only one rule to
follow: `:db/txInstant`s must be monotonically increasing, meaning you can never
insert a transaction timestamp that is the same or lower than any existing
transaction timestamp.

When you import existing data into Datomic, you might want Datomic to reflect
the fact that this data was created at various points in time. You can do that
by overriding `:db/txInstant` on the entity with the temporary id `"datomic.tx"`
using some `created-at` time you have from before. For example, I recently
designed a database to hold information about all my dinners over the past seven
years, which I've been keeping in an `org-mode` file. When I imported this data,
I set the meal time as the transaction time:

```clj
(require '[datomic.api :as d])

(def conn (d/connect "datomic:dev//localhost:4334/mydb"))

(d/transact conn [{:meal/recipes [[:recipe/id #uuid "58727d3b-6b13-4c47-a92a-6e441923715b"]]
                   :meal/diners [[:group/slug "hhv20b"]]
                   :meal/id #uuid "586aa8aa-65df-455d-9e43-409124bbe311"}
                  {:db/id "datomic.tx"
                   :db/txInstant #inst "2011-10-03T17:00:00Z"}])
```

Just make sure to always increase the tx time when you do this. When your app
goes into production, you can even keep controlling transaction times this way,
so long as you ensure it always increases. However, you are probably better off
letting Datomic do this for you.

## Annotating transactions with custom attributes

The transaction entity is just like any other Datomic entity: it's really just
an id that can be associated with any attribute. So at work, we defined the
following attributes:

```clj
[{:db/ident :tx/app-name
  :db/valueType :db.type/string
  :db/index true
  :db/cardinality :db.cardinality/one}

 {:db/ident :tx/app-version
  :db/valueType :db.type/string
  :db/index true
  :db/cardinality :db.cardinality/one}

 {:db/ident :tx/user
  :db/valueType :db.type/ref
  :db/cardinality :db.cardinality/one}

 {:db/ident :tx/puppet-master
  :db/valueType :db.type/ref
  :db/doc "Admin user working on behalf of another user"
  :db/cardinality :db.cardinality/one}

 {:db/ident :tx/ip-address
  :db/valueType :db.type/string
  :db/index true
  :db/cardinality :db.cardinality/one}]
```

When we write data to our database, we then add some or all of these attributes,
for increased insight into "who did what, when, and from where?":

```clj
(d/transact conn [{:some/app-data "Yeah"
                   :some/more-data "Oh, yep"}
                  {:another/entity 42
                   :another/piece-of-data "LOL"}
                  {:db/id "datomic.tx"
                   :tx/app-name (:docker-container-name config)
                   :tx/add-version (:docker-container-version config)
                   :tx/user current-user}])
```

This creates a highly detailed audit trail (per fact) with a reasonably small
overhead.

## Where does all the info come from?

You might be thinking that having this data would be useful, but that writing it
would be cumbersome at best. You probably do not want your entire model to have
a concept of an ip address of the acting user. Datomic's transaction model
actually enables a very elegant solution to this problem.

As you saw in the above example, a Datomic transaction is just a sequence of
either entity maps (like above), or transaction functions (like `[:db/add
entity-id attribute value]`). The whole thing must be created and passed to the
transactor as one piece of data. We used this to make pretty much our entire
model "pure" (as in "pure functions"). Any function conceptually "creates" or
"edits" data, instead simply produce transaction data. It basically compiles an
order of side-effects it wants carried out, like this:

```clj
{:success? true
 :tx-data [...]}
```

(Or `:success? false` along with validation errors, failures, and more). This
data structure is processed in _a single place_ in the entire app. If we're
making changes in response to a web request, this "place" will have access to
the request. From the request, we get the IP address and the current user. In
any case, it has access to the configuration, which contains information about
the current running version of the app. This way all data in our database is
annotated with what version of the app (git sha) put it there, along with a
bunch of other useful information. Along with Datomic's historic data record, we
could for instance include the database in a `git bisect` to track down
problems. Quite useful indeed.

I will be writing another post on how we process "commands" in our system.

[Follow me (@cjno) on Twitter](http://twitter.com/cjno)
