--------------------------------------------------------------------------------
:type :meta
:title Querying across Datomic databases
:published #time/ldt "2017-06-17T12:00"
:updated #time/ldt "2019-03-27T12:00"
:tags [:clojure :datomic]
--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title Querying across Datomic databases
:body

Among its many, many unique and interesting features, [Datomic](http://www.datomic.com/)
supports [datalog](http://docs.datomic.com/query.html) queries across multiple
databases. I recently had a chance to actually use this feature, and thought I'd
share an actual example of it.

--------------------------------------------------------------------------------
:type :section
:title Tracking recent activity
:body

In our app, people can make booking requests to venues by posting a form with
some data, including their email address. For ease of use, there is no need to
log in to use the service, but we do connect all requests from the same email
address to the same user entity (and use email token login for access). This
means that at the time when the form is posted, we don't know which user entity
is operating, but we do have their email.

We wanted to trigger an event if a user makes too many requests in too short a
time. Every time the form is posted, we'll query the database for the number of
requests from the same email address since some point in time.

## Querying temporal data in Datomic

Whenever you put data into Datomic, it also creates a transaction entity that is
tagged with the timestamp of when it was created. This information is fully
queryable, so we _could_ solve our problem the following way:

```clj
(require '[datomic.api :as d])

;; From the form
(def input {:booking-request/email "christian@cjohansen.no"
            ...})

(def conn (d/connect "datomic:dev://localhost:4334/mydb"))

(d/q '[:find (count ?e) .
       :in $ ?email ?since
       :where
       [?u :user/email ?email]
       [?e :booking-request/tenant ?u]
       [?e :booking-request/id _ ?initial-tx]
       [?initial-tx :db/txInstant ?created-at]
       [(< ?since ?created-at)]]
     (d/db conn)
     (:booking-request/email input)
     #inst "2017-06-10T12:00:00")
```

This query uses Datomic's queryable transaction time to find the relevant
requests. First we find the user entity corresponding to the email address of
the user (passed in to `?email` from `(:booking-request/email input)`), then we
find all the booking request entities `?e` where this user is the tenant. Then
we find the id of the transaction, `?initial-tx`, that created this request
(`:booking-request/id` is only ever written on initial create), join with its
`:db/txInstant`, and filter out requests created before the specified point in
time. This is already a pretty neat query.

## Filtering the database

Most of the time, `:where` conditions on `:db/txInstant` can be solved in a
more reusable (and performant) way. Instead of filtering inside the query, we
can _filter the entire database_, and run an unfiltered query on top. This
solution is better for mainly two reasons:

1. The query can be used as is on any database - your entire current state, a
   historic one, or some other filtered sub set.
2. The query only considers a subset of the facts in the database, making for a
   potential boost in performance

To query only facts after a certain point in time, we use the `(d/since db
since)` function:

```clj
(d/q '[:find (count ?e) .
       :in $ ?email
       :where
       [?u :user/email ?email]
       [?e :booking-request/tenant ?u]]
     (d/since (d/db conn) #inst "2017-06-10T12:00:00")
     (:booking-request/email input))
```

This query is *much* shorter, and more concisely describes the actual problem
we're solving. Unfortunately, it returns `nil` almost no matter how you hold it.

The problem is that when you filter the database this way, you end up with
_only_ the facts that were inserted after the specified point in time. In this
case, the user was created before that time, and simply isn't a part of this
particular database/view. To mitigate, we can query two databases: Find the
requests in the filtered database, and the user in the entire database. This can
be achieved by simply passing multiple databases to the query. When we do, we
must name them (other than `$`) and be explicit about which database to match
individual facts with.

```clj
(let [db (d/db conn)]
  (d/q '[:find (count ?e) .
         :in $recent $users ?email
         :where
         [$users ?u :user/email ?email]
         [$recent ?e :booking-request/tenant ?u]]
       ;; referred to as $recent above
       (d/since db #inst "2017-06-10T12:00:00")
       ;; referred to as $users above
       db
       (:booking-request/email input)))
```

And there you go. A reusable query that can be used to find a user's activity in
any time duration using database filtering functions like `(d/as-of db time)`
and `(d/since db time)` (or a combination), and that performs a join across
databases. Did I mention that I love working with Datomic?

[Follow me (@cjno) on Twitter](http://twitter.com/cjno)
