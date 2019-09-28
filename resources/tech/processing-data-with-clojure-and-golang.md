--------------------------------------------------------------------------------
:type :meta
:title Processing data with Clojure and Go
:published #time/ldt "2018-04-20T12:00"
:tags [:clojure :go :spotify]
--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title Processing data with Clojure and Go
:body

I recently wrote about [a better playlist shuffle with
Go](/a-better-playlist-shuffle-with-golang/), where I presented my ideal
implementation of a "shuffle" that isn't very random at all. In that post I
outlined the algorithm and showed off my implementation in Go (one of my
earliest uses of said language). In this post I'll present the same algorithm in
Clojure, compare parts to the Go implementation, and muse a little about the
difference between these two languages when it comes to data processing
(performance, typing, etc).

--------------------------------------------------------------------------------
:type :section
:title First index
:body

Unlike Go, Clojure has `indexOf` for collections by calling out to the Java
method:

```clj
(.indexOf [:a :b :c :d :e] :c) ;;=> 2
```

However, for the `distribute` algorithm we really need `first-index-of` that
starts at a given index, and wraps around if necessary. Like so:

```clj
(defn first-index-of [coll item start]
  (let [idx (.indexOf (drop start coll) item)]
    (if (< idx 0)
      (.indexOf coll item)
      (+ idx start))))
```

## Distribute Items

The first integral part of the algorithm is the `distribute` function, which
takes a collection, a label (e.g. genre name, artist name, etc), and a list of
items (e.g. tracks) to distribute evenly across the available positions in the
collection. `nil` marks available positions.

You might remember the Go version, that used `for` without any loop conditions:

```go
func Distribute(distribution []string, artist string, tracks int) []string {
  stepSize := float64(len(distribution)) / float64(tracks)
  index := 0
  remainder := stepSize

  for {
    if tracks == 0 {
      return distribution
    }

    if remainder >= stepSize {
      index = IndexOf(distribution, "", index)
      distribution[index] = artist
      remainder -= stepSize
      tracks--
    }

    index++
    remainder++
  }
}
```

The Clojure version is essentially the same, except it uses `loop` in place of
`for`, which is a similar but not the same construct. `loop` works more like a
recursive function. Instead of modifying local variables, which is something you
practically don't do in Clojure, it creates new values and passes them to the
next iteration:

```clj
(defn distribute [dist [bucket items]]
  (let [step-size (/ (count dist) (count items))]
    (loop [idx 0
           n (count items)
           remainder step-size
           dist (into [] dist)]
      (cond
        (= n 0) dist

        (<= step-size remainder)
        (let [idx (first-index-of dist nil idx)]
          (recur (inc idx) (dec n) (inc (- remainder step-size)) (assoc dist idx bucket)))

       :default (recur (inc idx) n (inc remainder) dist)))))
```

A few notes:

* Division of two integers in Clojure will produce a `clojure.lang.Ratio` if the
  result is not an integer so there is no need for the type cast from the Go
  version
* `(into [] distribution)` ensures the distribution is associative, so that
  `assoc` won't throw an error, as it would with e.g. seqs and lists.
* The function's signature is slightly different: instead of receiving a label
  and a count of items, we expect a tuple of `[bucket items]` for practical
  reasons that will become evident shortly.
* In the Go version, this function receives the bucket/label and the count of
  items - this was done to avoid choosing between copying the slice as it is
  passed or passing a slice of pointers (which means the caller can't trust the
  function not to mutate items).

## Distributing By Artist

Next up, we'll distribute tracks by artist. In Go this looked like:

```go
func DistributeByArtist(items []Track) []Track {
  grouped := GroupBy(items, func (track Track))
  buckets := bucketsByOccurrences(grouped)
  distribution := make([]string, len(items))

  for _, bucket := range buckets {
    Distribute(distribution, bucket[0], len(bucket[1]))
  }

  return ReifyDistribution(distribution, grouped)
}
```

The Clojure version is conceptually very similar:

```clj
(defn distribute-by-artist [tracks]
  (let [grouped (group-by :artist tracks)
        distribution (->> grouped
                          (sort-by #(count (second %)))
                          reverse ;; Most frequently recurring artists first
                          (reduce distribute (map (constantly nil) tracks)))]
    (reify-distribution distribution grouped)))
```

There are a few very important differences:

* The Clojure version is self-contained: The standard library provides usable
  `group-by` and `sort-by` functions, as well as keywords that are functions
  that look themselves up in their argument
* The Go version relies on ~50 lines of custom code to perform grouping and sorting
* The Clojure version relies entirely on persistent data structures

## Distribute By Anything

I closed off the previous article wanting for a solution that would distribute
my list by any number of factors: genres, artists, albums, and so on. In
Clojure, such an extension to `distribute-by` looks like:

```clj
(defn distribute-by [items fns]
  (if-not (first fns)
    items
    (let [grouped (group-by (first fns) items)
          distribution (->> grouped
                            (sort-by #(count (second %)))
                            reverse
                            (reduce distribute (map (constantly nil) items)))
          groups (->> grouped
                      (map #(vector (first %) (distribute-by (second %) (rest fns))))
                      (into {}))]
      (reify-distribution distribution groups))))
```

Because keywords implement `IFn`, it can be called as such:

```clj
(distribute-by playlist [:genre :artist :album])
```

It should be noted that this solution uses a relatively inefficient form of
recursion, both in the above Clojure implementation, and my [Go
implementation](https://github.com/cjohansen/shufflify/blob/2e07478078d69b7a1a5fddeecd6819f82133f453/spotify-service/shuffle/shuffle.go#L118).
In practice it will only recur a handful of times, so it's not really worth
worrying about.

## Reify Distribution

The final step is to reify the distribution: replace the bucket markers in the
distribution with actual items. This was straight-forward in Go:

```go
func ReifyDistribution(distribution []string, groups GroupedItems) []Item {
  items := make([]Item, len(distribution))

  for i, key := range distribution {
    items[i], groups[key] = groups[key][0], groups[key][1:]
  }

  return items
}
```

This is the only function that is longer in my Clojure version, as it cannot
mutate its operands as it goes:

```clj
(defn reify-distribution [positions grouped]
  (loop [positions positions
         data grouped
         res []]
    (if (= (count positions) 0)
      res
      (let [bucket (first positions)]
        (recur (drop 1 positions)
               (update-in data [bucket] #(drop 1 %))
               (conj res (first (get data bucket))))))))
```

I'm sure smarter people than me could write it more compactly.

## Differences

So, what are the main differences? The way I see it, there are three interesing
aspects to consider:

1. Expressiveness/generality
2. Performance
3. Static vs dynamic types

### Expressiveness

There is no doubt that overall, the Clojure implementation is much more terse,
and relies more heavily on existing funcitonality from its standard library.
Whether you consider an advantage will likely depend on prior experience: people
unfamiliar with Clojure will probably find this version somewhat cryptic and
maybe too terse. Someone on the other side might consider the Go implementation
somewhat inelegant and "manual".

Some qualities can be assessed objectively:

* The Clojure version implements fewer features/relies more heavily on the
  standard library
* The Clojure version has fewer lines of code
* The Clojure version is more generic than the Go one, for better or for worse
* The Go version is immune to certain kinds of misuse through its static types
* The Go version more clearly conveys what data types it operates on

Personally, I find it much more natural to process data with Clojure, but I am
heavily biased towards its functional and dynamically typed nature.

### Performance

There is no doubt that the Go version is faster. I wrote a simple benchmark
using Go's built-in benchmark tool like so:

```go
var benchPlaylist []Item = []Item{
  // ...
}

var gaa []func(Item) string = AttributeAccessors([]string{"genre", "artist", "album"})

func BenchmarkShuffleBy(b *testing.B) {
  for i := 0; i < b.N; i++ {
    ShuffleBy(benchPlaylist, gaa)
  }
}
```

This shuffles a 70 track playlist by genre, artist, and album in about 40.000
nanoseconds. I'm not really sure what a good way to benchmark Clojure code is,
what with JVM warm up and all, but some tinkering with the REPL and `time` yields
a result an order of magnitude slower. However, we're still talking less than a
millisecond to shuffle what I consider a realistic playlist.

While Go is clearly faster, performance is good enough in both implementations
for the target problem domain to be nearly irrelevant.

### Static vs Dynamic Types

I'm not going to attempt reiterating this debate. During my career, I have
mostly used dynamic languages, know how to use them well, and have a clear (if
somewhat unfounded) bias towards them. I don't really have enough static typing
experience to present an informed argument.

Whenever I work in a statically typed language like Go, I always appreciate the
documentation effect of typed function signatures. Coming back to a piece of
Clojure code way after it was written, it can often be hard to know exactly what
a certain function expects to find in a generic map. Luckily, Clojure now has
[clojure.spec](https://github.com/clojure/spec.alpha), which can partially
bridge the gap.

Unfortunately, the kind of code demonstrated in this and the previous post don't
benefit hugely from typing in my opinion. Had Go supported generics, we could've
forgone the `Item` type, and written a fully generic solution as such:

```go
func ShuffleBy(items []T, fns []func(T) string) []T
```

Clojure Spec has little to offer in such generic cases. We _could_ have done
this:

```clj
(require '[clojure.spec.alpha :as s])

(s/def ::items coll?)
(s/def ::fns (s/coll-of ifn?))

(defn shuffle-by [items fns]
  (start-randomly (distribute-by (shuffle items) fns)))

(s/fdef shuffle-by
        :args (s/cat :items ::items :fns ::fns)
        :ret ::items)
```

Using
[instrumentation](https://clojure.org/guides/spec#_instrumentation_and_testing)
this could provide some very basic error-handling development time, but I don't
think this is the most efficient use of spec.

Spec really shines for code that has more specific expectations of our data.
Imagine we'd used our generic constructs to create a highly specific interface:

```clj
(defn shuffle-playlist [playlist]
  (shuffly-by playlist [:genre :artist :album]))
```

This is a function that would benefit more from some structural hinting:

```clj
(s/def ::genre string?)
(s/def ::artist string?)
(s/def ::album string?)
(s/def ::track string?)
(s/def ::track (s/keys :req-un [::genre ::artist ::album ::track]))
(s/def ::playlist (s/coll-of ::track))

(defn shuffle-playlist [playlist]
  (shuffly-by playlist [:genre :artist :album]))

(s/fdef shuffle-playlist
        :args (s/cat :playlist ::playlist)
        :ret ::playlist)
```

When the function has more specific requirements about the shape of data passed
to it, spec provides lots of useful documentation, and can even provide some
additional direction during development and tests. With
[expound](https://github.com/bhb/expound) and
[Orchestra](https://github.com/jeaye/orchestra) it can even provide full
instrumentation with nice human-readable error messages.

### Dynamic Go Code

In the same way that Clojure can gradually add type hints and optionally enforce
invariants on data, Go could be bent to be more dynamic. I've said several times
that the Clojure implementation is more generic than the Go one, and it's true.
It became this way as I tried to leverage each language's strengths, and write
somewhat idiomatic code. However, we _could_ make everything an `interface{}` in
Go, and write a version that would be as dynamic as the Clojure one. I'm pretty
sure it would still match up performance-wise, but it still feels... wrong.

## Parting Thoughts

This has been an interesting experience. I've gotten to know Go a lot better. I
have a pretty good grasp on it's strength and weaknesses, particularly when it
comes to data modelling and processing. In this specific example, the single
biggest differentiator is the standard library. By extension this includes
either dynamic vs static typing _or_ the lack of generics in Go. Clojure also
lacks generics, but it isn't as much of a problem as it's dynamically typed. In
either case, the Clojure standard library includes many more data processing
building blocks, which ultimately allows you to perform such tasks in a more
expressive and terse manner.
