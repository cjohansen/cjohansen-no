# Referentially transparent CRUD

Side-effects makes software hard to manage. They bring in external dependencies,
represent an unknown that is hard to reason about, and is a source of
instability that can be hard to control. Testing side-effecting code is *much*
harder than testing pure functions. For these reasons I prefer to write as many
pure functions as possible, and contain side-effects in as few functions as
possible.

Clojure and its persistent data structures has helped me increase the amount of
pure functions in my code bases a lot. However, I/O invariably rears its ugly
head and ruins the party. Luckily, [Datomic](http://www.datomic.com/) has
several features that go a long way in enabling you to write referentially
transparent code, even when you're I/O-ing.

## Reading data

When you read data from Datomic, you need "a database". "A database" is
different from "the database" (which I guess is called "the catalog" in Datomic
lingo). "A database", in Datomic terms, is an immutable value representing
some subset of the facts in your database. Typically, you will use the view
provided by `(d/db conn)`, which is a compressed view of the current state of
your database. It's compressed because facts that have been overwritten are
excluded from it.

Representing the database with an immutable value allows all your read
operations to become pure. For instance, consider the following function:

```clj
(defn tenant?
  [db user]
  (< 0 (or (d/q '[:find (count ?p) .
                  :in $ ?u
                  :where [?p :booking-request/tenant ?u]]
                db user)
           0)))
```

This function accepts a database value and the id (or lookup ref) of a user, and
tells you whether or not that user has ever been a tenant. The function is
referentially transparent, because it computes its answer solely from its
arguments, and both the arguments are immutable values. Score!

## Writing data

Writing data in Datomic means calling `(d/transact conn)`, and the peer
connection is _not_ an immutable value. Datomic transactions consist of a
sequence of either transactor function invocations or entity maps. You compile
everything that goes into a transaction into one data structure, and send it off
in one go. This means that all the work that goes into putting data into the
database actually goes into building an immutable data structure that is put
into the database with one function call. By moving that last function call out
of the various domain functions that create data, they can all become
referentially transparent, and we can contain side-effects in one place.

### The data processing contract

Functions that create data typically perform one or more of the following steps:

- Validate data
- Look up additional data
- Process data
- Commit changes to database

One or more of these steps may find problems that prevent the changes from being
committed. This means that these data-creating functions cannot just return
transaction data, they also need to be able to communicate potential errors and
validation failures.

Let's define a contract:

```clj
{:command/success? true ;; or false
 :command/validation-data {}
 :command/tx-data []
 :command/error {}}
```

This allows us to easily know if a function succeeded in carrying out a command
or not. If it didn't, we'd expect there to be either `:command/validation-data`
or a `:command/error`. If it does succeed, it _might_ have some `:tx-data` that
can be sent to `(d/transact conn tx-data)`. The beauty of this is that it would
be trivial to add other kinds of side-effects as well, like `:command/emails`.

## Creating side-effects as commands

Let's try the command payload in a function. For our example, let's create a
user:

```clj
(require '[clojure.spec.alpha :as s]
         '[myapp.specs :as spec])

(s/def :user/fullname string?)
(s/def :user/email ::spec/email)
(s/def ::user (s/keys :req [:user/fullname :user/email]))

(defn create-user [input]
  (let [user (s/conform ::user input)]
    (if (= user :clojure.spec/invalid)
      {:command/success? false
       :command/validation-data (s/explain-data ::user input)}
      {:command/success? true
       :command/tx-data [(select-keys input [:user/fullname
                                             :user/email])]})))
```

We use `clojure.spec` for validation. If the input fails validation, we return
the spec explain data as the validation data. This could be further transformed
into human-digestible validation messages on the caller side.

If the command finds nothing wrong with the passed in user, it generates the
tx-data. But what if there already is a user with that email address? We need to
confer with the current database:

```clj
(defn create-user [db input]
  (let [user (s/conform ::user input)]
    (cond
      (= user :clojure.spec/invalid)
      {:command/success? false
       :command/validation-data (s/explain-data ::user input)}

      (d/entity db [:user/email (:user/email user)])
      {:command/success? false
       :command/validation-data {:clojure.spec/problems [{:via [:user/email]
                                                          :pred :user-email-unique}]}}

      :default
      {:command/success? true
       :command/tx-data [(select-keys input [:user/fullname
                                             :user/email])]})))
```

This function reads from the **immutable database value** to figure out if the
email is taken, and is still referentially transparent. It is however, not
atomic. If you need this check to happen atomically, it needs to happen inside
the transaction with a transactor function. Users likely won't be signing up in
such a tempo as to make that worth it, so we won't worry about it.

## Multi pass writes

Ok, so far so good. But what about those functions that create some data,
perform some more logic, then creates some more data? How can we break those
apart? Datomic to the rescue again.

Let's say we have two functions that create a command result like above, and we
want to exercise them one after another, but the second one needs to consider
the results of the first one. Datomic provides the `with` function for
speculatively applying transaction data to a database without commiting it to
the transaction log:

```clj
(def res1 (create-user db input))
(def res2 (create-other-entity (:db-after (d/with db (:tx-data))) other-input))
```

If you need to cross-reference entities between these two result sets, you can,
but it is a little bit finicky:


```clj
(defn create-user-with-foos [db input]
  (let [res (create-user db input)]
    (if-not (:command/success? res)
      res
      (let [{:keys [db-after tempids]} (d/with db (:command/tx-data res))
            res2 (add-foos db-after (get tempids "user") input)]
        (if-not (:command/success? res2)
          res2
          {:command/success? true
           :command/tx-data (mapcat :command/tx-data [res res2])})))))
```

This has the unfortunate limitation that you need to know what temporary ids
`create-user` operates with, so would only be appropriate functions that are
locally private to eachother. In most cases the use of `:tempids` can be avoided
with unique lookups:

```clj
(defn create-user-with-foos [db input]
  (let [res (create-user db input)]
    (if-not (:command/success? res)
      res
      (let [{:keys [db-after]} (d/with db (:command/tx-data res))
            res2 (add-foos db-after [:user/email (:user/email input)] input)]
        (if-not (:command/success? res2)
          res2
          {:command/success? true
           :command/tx-data (mapcat :command/tx-data [res res2])})))))
```

This version uses the `:user/email` attribute to lookup the newly created user
in the database that speculatively contains the recent user transaction.

There is still the pitfall that if the second command uses an entity id created
in the `d/with` "speculation", it may not be a valid id in the resulting
transaction. This can be partially mitigated by using the `:tempids` from the
first result to convert entity ids in the second result set back to tempids:

```clj
(defn create-user-with-foos [db input]
  (let [res (create-user db input)]
    (if-not (:command/success? res)
      res
      (let [{:keys [db-after tempids]} (d/with db (:command/tx-data res))
            res2 (add-foos db-after [:user/email (:user/email input)] input)]
        (if-not (:command/success? res2)
          res2
          (let [id->tmp (into {} (map (fn [[k v]] [v k]) tempids))]
            {:command/success? true
             :command/tx-data (clojure.walk/postwalk
                               #(or (id->tmp %) %)
                               (mapcat :command/tx-data [res res2]))}))))))
```

This *still* has a pitfall though. In order for this to work right, you need to
use explicit tempids for new entities, or you might still end up with an entity
id in the transaction that won't exist when transacting the data.

## Composing commands

In the previous example, you can see that there is a bit of mechanic "noise"
associated with the serial execution of commands. The mechanics can be
extracted, leaving us with a clean way of composing commands. Let's start with a
function that assumes commands are not dependent on each other's resulting
transaction data. The function will take a sequence of functions that return
commands. It will then execute the functions sequentially, and stop on the first
one that fails. If all of them succeed, the function returns their combined
result.

```clj
(defn exec-commands
  "Executes a seq of commands in sequence, returning either the first failure,
   or the combination of all successes"
  [xs & args]
  (loop [xs xs
         res []]
    (if (empty? xs)
      {:command/success? true
       :command/tx-data (mapcat :command/tx-data res)}
      (let [command-res (apply (first xs) args)]
        (if-not (:command/success? command-res)
          command-res
          (recur (rest xs) (conj res command-res)))))))
```

We can use this function to break up our user creation example into three
separate commands: validate the user, create the user, and add "foo" to the
user:

```clj
(defn validate-user [input]
  (let [user (s/conform ::user input)]
    (if (= user :clojure.spec/invalid)
      {:command/success? false
       :command/validation-data (s/explain-data ::user input)}
      {:command/success? true})))

(defn create-user [db input]
  (let [user (s/conform ::user input)]
    {:command/success? true
     :command/tx-data [(select-keys input [:user/fullname
                                           :user/email])]}))

(defn add-foos [db input]
  (if-let [lookup-ref [:user/email (:user/email input)]
           user-id (:db/id (d/entity db lookup-ref))]
    {:command/success? true
     :command/tx-data [[:db/add lookup-ref :user/foo (:user/foo input)]]}
    {:command/success? false}))

(defn create-user-with-foos [db input]
  (exec-commands [validate-user
                  (partial create-user db)
                  (partial add-foos db)]
                 input))
```

This is pretty nice. We've broken the user creation process into three short,
focused, and reusable functions. In the process we've done away with all nested
branching.

We could device a similar function that feeds the `tx-data` from one command
into the next, and resolve all the tempids in the end:

```clj
(defn exec-commands-incrementally
  "Executes a seq of commands in sequence, applying the tx-data from one command
  to the database passed to the next. Returns either the first failure, or the
  combination of all successes"
  [xs db & args]
  (loop [xs xs
         db db
         all-tempids {}
         res []]
    (if (empty? xs)
      (let [id->tmp (into {} (map (fn [[k v]] [v k]) all-tempids))]
        {:command/success? true
         :command/tx-data (clojure.walk/postwalk #(or (id->tmp %) %)
                                                 (mapcat :command/tx-data res))})
      (let [command-res (apply (first xs) db args)]
        (if-not (:command/success? command-res)
          command-res
          (let [{:keys [db-after tempids]} (d/with db (:command/tx-data command-res))]
            (recur (rest xs)
                   db-after
                   (merge all-tempids tempids)
                   (conj res command-res))))))))
```

Now this function comes with two major caveats that should make you think twice
before using it (I don't, but I do use the previous one a lot):

1. All commands must accept the db as their first argument
2. If you use `:db/id` on existing entities when building transactions, *all*
   new entities must have an explicit tempid

The first caveat isn't so bad, and the second one can be mitigated by always
using unique lookup refs instead of entity ids, e.g.:

```clj
[[:db/add [:user/email "christian@kodemaker.no"] :some/attr "Some value"]]
```

Still, this function does feature a few traps, while the previous does not.

## Processing command results

Ok, so we have some tools for generating side-effects, but we are not really
applying those effects yet. To transact the data, this should do the trick:

```clj
(defn process-result
  [result]
  (if (:command/success? result)
    (try
      (let [tx-result (and (seq (:command/tx-data result))
                           @(d/transact conn (:command/tx-data result)))]
        (assoc result :command/tx-result tx-result))
      (catch Exception e
        (log/error "Failed to process command results")
        (log/error e)
        (-> result
            (assoc :command/success? false)
            (assoc :command/error "Unexpected error processing results"))))
    result))
```

On the project I'm currently working on, we
[annotate transactions](/annotating-datomic-transactions/) with stuff like the
currently logged in user and their IP, along with some configuration data. For
this reason, our `process-result` also takes a `req`, which may be `nil`. It is
passed to a function that generates annotations:

```clj
(defn process-result
  [req result]
  (if (:command/success? result)
    (try
      (let [tx-result (and (seq (:command/tx-data result))
                           @(d/transact conn (concat (:command/tx-data result)
                                                     (db/tx-annotations req))))]
        (assoc result :command/tx-result tx-result))
      (catch Exception e
        (log/error "Failed to process command results")
        (log/error e)
        (-> result
            (assoc :command/success? false)
            (assoc :command/error "Unexpected error processing results"))))
    result))
```

## A command web handler

When we've designed such a nice streamlined API for handling side-effects, we
need to make sure that API endpoints in the app use them as well. In the app
where these ideas originated, we don't have a full-featured REST API for the
client to use. When the server is only serving its own client, we can afford
ourselves a network API with less ceremony than REST.

Our app has a single endpoint for enacting side-effects, and that is the command
endpoint, `/api/command/:command-id`. The client posts a command here, the
backend checks that the current user is authorized to execute the named command
on the provided parameters, and the command function is called, the results are
processed, and finally an HTTP response is compiled and sent back to the client.
This endpoint is used for validations, creating stuff, editing stuff, you name
it.

The HTTP handler looks like this:

```clj
(defn handler [req]
  (let [command {:command/user (:user req)
                 :command/params (:params req)
                 :command/id (keyword (-> req :routing :id))}]
    (->> command
         execute
         (process-result req)
         (http-response req command))))
```

We saw `process-result` before. `execute` looks like this:

```clj
(defmulti execute
  (fn [command]
    (log/info "command/execute" (:command/id command) (pr-str command))
    (:command/id command)))
```

This is a multimethod that dispatches on the `:command/id`. To expose a command
for creating users over HTTP, we would do something like this:

```clj
(require '[myapp.command :as command])

(defmethod command/execute :create-user [{:keys [command/params]}]
  (create-user-with-foos (d/db conn) params))
```

I guess you noticed the log statement in `execute`. In our code-base we have a
similar log statement in `process-result`, which was all that was needed to have
**the input and resulting side-effects of every single command in our logs**.

### The HTTP response

The final part of the puzzle is the `http-response` function, which looks at the
resulting data structure and prepares a response for the client:

```clj
(defn http-response [req command result]
  (let [resp (if (:command/success? result)
               {:status 200
                :body (http-response-body command result)}
               {:status (if (false? (:command/valid? result)) 400 500)
                :body (select-keys result [:command/success?
                                           :command/valid?
                                           :command/error
                                           :command/validation-data])})]
    (when-not (:command/success? result)
      (log/warn "Unsuccessful command" command result))
    resp))
```

In practice, our implementation does a few more things. First of all, command
results can have a `:command/http-status`, allowing e.g. the user creation
command to respond with a `201`. Some commands need to control the user session,
so we also support the `:command/session` key:

```clj
(defn http-response [req command result]
  (let [resp (if (:command/success? result)
               {:status (or (:command/http-status result) 200)
                :body (http-response-body command result)}
               {:status (cond
                          (:command/http-status result) (:command/http-status result)
                          (false? (:command/valid? result)) 400
                          :default 500)
                :body (select-keys result [:command/success?
                                           :command/valid?
                                           :command/error
                                           :command/validation-data])})]
    (when-not (:command/success? result)
      (log/warn "Unsuccessful command" command result))
    (if-let [session (:command/session result)]
      (assoc resp :session session)
      resp)))
```

The observant reader will notice the call to `http-response-body`. This is yet
another multimethod, which allows us to implement a separate function to
calculate the response body for certain commands.

Here is an actual example of one of our commands:

```clj
(defmethod command/execute :create-new-booking-request
  [{:keys [command/user command/params]}]
  (input/create (d/db conn)
                (dissoc params :venue-slug)
                [:venue/slug (:venue-slug params)]))

(defmethod command/http-response-body :create-new-booking-request
  [command {:keys [command/tx-result]}]
  {:command/success? true
   :command/data (->> (get-in tx-result [:tempids "booking-request"])
                      (d/pull (:db-after tx-result) booking-request-pull-spec)
                      ref/encode
                      format-contact-tel)})
```

I have not included all the details, but the main point here is how we've
created a system for completely referentially transparent CRUD.

## In summary

So there you have it. I'm sure you can create referentially transparent
CRUD-functions in most any language, but Clojure and Datomic both bring features
to the table that make doing so easy. In fact, they both encourage this style of
programming. The result is that testing I/O functions is really straight
forwards and fast. Because it's all just pure functions, reasoning about the
code is quite easy, and it's just damn fun to work with.

Did I mention that our app handles emails and sms messages the same way? Add
stuff to `:command/emails`, and `process-results` will send it off for you.

Clojure. Datomic. You gotta love 'em.

June 17th 2017

[Follow me (@cjno) on Twitter](http://twitter.com/cjno)