# Clojure to die for

There are many reasons to love [Clojure](http://clojure.org);
[homoiconicity](http://en.wikipedia.org/wiki/Homoiconicity),
[persistent data structures](http://en.wikipedia.org/wiki/Persistent_data_structure),
[macros](http://clojure.org/macros), and the list goes on. In this post, I want
to focus on a few "details" in the language that really puts the icing on the
cake, and clears up many common situations for me.

## Map access
<a name="maps"></a>

In Clojure, you can access map entries in a number of useful ways. First, a map
can be called as a function with a key as the only argument. It will return the
value associated with the key, or `nil`, if there are none:

```clj
(def person {:name "Christian"})
(person :name) ;=> "Christian"
(person :age) ;=> nil
```

Usually, maps use keywords for keys. Keywords can also be used as functions,
taking a map as their only argument:

```clj
(def person {:name "Christian"})
(:name person) ;=> "Christian"
(:age person) ;=> nil
```

This may be subjective, but to me this reads better. However, the real reason
why this is cool is because of this:

```clj
(nil :key)
;; CompilerException java.lang.IllegalArgumentException: Can't call nil, compiling:(/private/var/folders/56/h3kfyd9n0r16f42bf4swkszm0000gn/T/form-init2716363333658506112.clj:1:1)
(:key nil)
nil
```

In other words, using keywords to access map items produces `nil` if your map is
actually `nil`. This turns out to be very practical.

Here's another wicked cool thing about keywords-as-functions:

```clj
(def people [{:name "John Doe"}
             {:name "Jane Doe"}])
(map :name people)
["John Doe" "Jane Doe"]
```

### Deep access

Whenever you have nested maps (or vectors in maps in vectors for that matter),
Clojure offers multiple functions to make life easier. The first is
`get-in`:

```clj
(def person {:name "Christian"
             :address {:country "Norway" :city "Oslo"}})

(get-in person [:name])
"Christian"
(get-in person [:address :country])
"Norway"
(get-in person [:address :street])
nil
(get-in nil [:address :street])
nil
```

`get-in` also sorta works when maps and vectors mix:

```clj
(def people [{:name "Christian" :hobbies [{:name "Beer"}
                                          {:name "Food"}
                                          {:name "Music"}]}])

(get-in people [0 :hobbies 1 :name])
```

Beware though! Using `get-in` on a lazy sequence will not work as expected.

There are functions available to help you "set" nested map entries as well.
`assoc-in` is used to put a value into the map/vector structure
somewhere, while `update-in` can be used to set a value based on the
old one (e.g. you provide a function that receives the old value as its
argument, and may return the "new" value):

```clj
(assoc-in person [:address :street] "Street-o-rama")
{:name "Christian",
 :address {:country "Norway", :city "Oslo", :street "Street-o-rama"}}

(update-in person [:name] (fn [name] (.toUpperCase name)))
{:name "CHRISTIAN", :address {:country "Norway", :city "Oslo"}}
```

## The anonymous function literal
<a name="anon-fn"></a>

The anonymous function literal is
a [dispatch macro](http://clojure.org/reader#toc2). Basically, it is a very
compact way of creating functions, perfect for those cases where you need a
quick one-off function. It is perfect for use with the aforementioned
`update-in`.

```clj
;; This:
(update-in person [:name] (fn [name] (.toUpperCase name)))
{:name "CHRISTIAN", :address {:country "Norway", :city "Oslo"}}

;; ...can be more succinctly written like this:
(update-in person [:name] #(.toUpperCase %))
{:name "CHRISTIAN", :address {:country "Norway", :city "Oslo"}}
```

By using the anonymous function literal, we avoid one nested form. If the
function only receives one argument, you can refer to it as `%`. If the function
should take more arguments, refer to them with `%1`, `%2` etc. `%` is the same
as `%1`, but I prefer not to mix (e.g. `%` and `%2` in the same form looks
inconsistent and unappealing to me).

The anonymous function literal is also a good match for map. Let's say you are
looking for the number of leading spaces on each line in a block of text. You
might write it like so:


```clj
(map (fn [line] (count (re-find #"^ +" line))) lines)
```

In short concise code snippets like this, the function contributes unnecessary
amounts of noise. The anonymous function literal can clean it up:

```clj
(map #(count (re-find #"^ +" %)) lines)
```

This has less noise, and communicates the mapping much clearer.

## The thread-first macro
<a name="thread-first"></a>

The thread-first macro is a feature that is designed to help you untangle stuff
like this:

```clj
(prepare-pages
 (pages/get-pages
  (content/cultivate-content
   (validate-raw-content
    (content/load-content)))))
```

Using the thread-first macro, we can break up this deeply nested structure and
represent it as a pipeline instead:

```clj
(-> (content/load-content)
      validate-raw-content
      content/cultivate-content
      pages/get-pages
      prepare-pages)
```

This is easier to read because it is sequential instead of nested. It is also
easier to manipulate.

The thread-first macro gets its name from the fact that it threads the previous
value in as the first argument to the next form. This becomes clear if any of
the forms actually take arguments:

```clj
(-> person
    :address
    (assoc :street "Street-o-rama")
    (assoc :city "Gotham"))
```

Here we're also using keywords-as functions. This form means the same as this:

```clj
(assoc (assoc (:address person) :street "Street-o-rama") :city "Gotham")
```

I think it is fairly uncontroversial to say that the thread-first form is way
easier to understand.

Remember the not-so-optimal example of using `get-in` on a potentially lazy
sequence? We can do the same thing using the threading macro for an elegant
solution that is also performant:

```clj
;; Original example
(get-in people [0 :hobbies 1 :name])

;; Respects lazy sequences
(-> people first :hobbies second :name)
```

This will still silently return `nil` if for instance this person has no
hobbies.

## The thread-last macro
<a name="thread-last"></a>

The thread-last macro is just like the thread-first, except it threads values as
the last argument to next form:

```clj
(->> lines
     (remove empty?)
     (map #(count (re-find #"^ +" %)))
     (min*))

;; Which is the same as
(min* (map #(count (re-find #"^ +" %)) (remove empty? lines)))
```

If you like these, you'll be pleased to know
that [Clojure 1.5](https://github.com/clojure/clojure/blob/master/changes.md)
shipped with even more threading macros, such as `as-&gt;`, which allows you to
choose and position the placeholder yourself.

These are just some of the details I love about Clojure, but I use them a lot,
and they are for me a big part of what makes Clojure so effortless to work with.

[Follow me (@cjno) on Twitter](http://twitter.com/cjno)

## Discuss

- [Hacker News discussion](https://news.ycombinator.com/item?id=7377684)
