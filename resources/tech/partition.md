--------------------------------------------------------------------------------
:type :meta
:title Partitioning data
:locale :nb
:published #time/ldt "2020-12-21T12:00"
:tags [:functional-programming :clojure]
:description

Functions like `map`, `filter`, and `reduce` are useful tools that many
developers keep in their toolbox. `partition` may not be as commonly known, but
in this post I'll show you why it should be.

--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title Partitioning data
:body

Functions like `map`, `filter`, and `reduce` are useful tools that many
developers keep in their toolbox. `partition` may not be as commonly known, but
in this post I'll show you why it should be.

--------------------------------------------------------------------------------
:type :section
:body

Lists, or sequences, are incredibly useful data structures. A lot of the data we
process is already a sequential collection of things, and by representing them
with the same basic data structures, we can solve problems with the same tools
over and over again. Consider how useful `map` is. The more you use it, the more
uses you see for it.

- Collect the email address of a group of people: `(map :email users)`
- Render buttons for all the available actions: `(map Button actions)`
- Parse a bunch of strings to numbers: `(map parse-int strs)`

`filter` is equally useful once you get to know it.

In much the same way, partitioning data is a highly useful generic abstraction.
Imagine you have a list of 9 people, and you want to display them in a 3x3 grid.
Wouldn't it be neat if you could chop the list into three lists of three
elements? That's exactly what `partition` does if you pass `3` as its first
argument:

```clj
(def people
  ["Anne"
   "Arnold"
   "Ali"
   "Bertha"
   "Brianna"
   "Bob"
   "Carl"
   "Celine"
   "Kelly"])

(partition 3 people)
;;=> [["Anne" "Arnold" "Ali"]
;;    ["Bertha" "Brianna" "Bob"]
;;    ["Carl" "Celine" "Kelly"]]
```

Perfect! Now pass the result to `(map Row partitioned)` to render it. Using
Clojure's thread last macro, you end up with a nice pipeline:

```clj
(->> people
     (partition 3)
     (map Row))
```

--------------------------------------------------------------------------------
:type :section
:title Partitioning with a predicate
:theme :dark1
:body

Sometimes it is useful to partition by comparing elements. Consider this list of
fascinating events:

```clj
(def events
  [{:date "2020-04-29"
    :event "Woke up"}
   {:date "2020-04-29"
    :event "Made coffee"}
   {:date "2020-04-30"
    :event "Slept in"}
   {:date "2020-04-30"
    :event "Took a shower"}
   {:date "2020-04-31"
    :event "Day off"}])
```

We can partition this into a list of events per day by using `:date` as the
predicate for `partition-by`. This way a new list is started every time `:date`
yields a different value:

```clj
(partition-by :date events)

;;=> [[{:date "2020-04-29", :event "Woke up"}
;;     {:date "2020-04-29", :event "Made coffee"}]
;;
;;    [{:date "2020-04-30", :event "Slept in"}
;;     {:date "2020-04-30", :event "Took a shower"}]
;;
;;    [{:date "2020-04-31", :event "Day off"}]]
```

In place of `:date` we could have used any function of our liking, partitioning
the dataset by arbitrary rules.

--------------------------------------------------------------------------------
:type :section
:title Previous and next
:theme :light1
:body

`partition` also supports separating `step` from `n` - in other words, the
number of steps we move in the seq does not have to correspond to the group
size. This is useful when we want elements to belong to multiple groups.

Finding adjacent elements is a common use case. It is tempting to use some
looping structure and local state to find those, but it is not necessary.

We can partition with `n` set to 3, but `step` just 1. This moves just one step
ahead in the collection and makes a new group of 3 elements. The result is a
sequence of each element along with their adjacent elements:

```clj
(partition 3 1 (range 10))

;;=> [[0 1 2]
;;    [1 2 3]
;;    [2 3 4]
;;    [3 4 5]
;;    [4 5 6]
;;    [5 6 7]
;;    [6 7 8]
;;    [7 8 9]]
```

At least, that's _almost_ what we got. This list only contains 8 groups, but the
input list had 10. `partition` only makes full groups, so the groups that would
have started with 8 and 9 are excluded, as they would not be full.
`partition-all` isn't so picky:

```clj
(partition-all 3 1 (range 10))

;;=> [[0 1 2]
;;    [1 2 3]
;;    [2 3 4]
;;    [3 4 5]
;;    [4 5 6]
;;    [5 6 7]
;;    [6 7 8]
;;    [7 8 9]
;;    [8 9]
;;    [9]
```

This has the right number of elements, but isn't exactly right either. If we're
going to traverse this list and find `[prev x next]`, the last two elements will
not be helpful. We can solve this in one of two ways, depending on the desired
behavior.

--------------------------------------------------------------------------------
:type :section
:title Linear previous and next
:body

If "previous" from the first element and "next" from the last element are both
desired to be `nil`, then we can pad the collection with two `nil`s and call it
a day:

```clj
(->> (concat [nil] (range 10) [nil])
     (partition 3 1))

;;=> [[nil 0 1]
;;    [0 1 2]
;;    [1 2 3]
;;    [2 3 4]
;;    [3 4 5]
;;    [4 5 6]
;;    [5 6 7]
;;    [6 7 8]
;;    [7 8 9]
;;    [8 9 nil]]
```

Be aware that this is not advisable if you intend to partition a lazy dataset,
as the `concat` realizes the whole thing.

--------------------------------------------------------------------------------
:type :section
:title Circular previous and next
:theme :dark1
:body

In a circular structure, the last element is the previous from the first(!)
Similarly, the first element is the next from the last. We can achieve this by
padding the collection in both ends with the right data.

There are many ways to do this, here's one:

```clj
(let [xs (range 4)
      n (count xs)]
  (->> (cycle xs)
       (drop (- n 1))
       (take (+ n 2))))

;;=> [3 0 1 2 3 0]
```

In other words:

1. Repeat the elements of the list:
   ```clj
   ;;=> [0 1 2 3 0 1 2 3 0 1 2 3 ...]
   ```
2. Drop the entire first sequence, except for the last element:
   ```clj
   ;;=> [0 1 2 3 0 1 2 3 0 1 2 3 ...]
   ;;          ^
   ;;=> [3 0 1 2 3 0 1 2 3 ...]
   ```
3. Take as many elements as the original list had, plus two (one in each end):
   ```clj
   ;;=> [3 0 1 2 3 0]
   ```

Then partition the result with step 1 and size 3:

```clj
(let [xs (range 10)
      n (count xs)]
  (->> (cycle xs)
       (drop (- n 1))
       (take (+ n 2))
       (partition 3 1)))

;;=> [[9 0 1]
;;    [0 1 2]
;;    [1 2 3]
;;    [2 3 4]
;;    [3 4 5]
;;    [4 5 6]
;;    [5 6 7]
;;    [6 7 8]
;;    [7 8 9]
;;    [8 9 0]]
```

And there you go! The entire list is found in the middle position of the tuples,
with the adjacent elements in position 0 and 2. With this trick up your sleeve,
you can leave even more imperative looping constructs behind in favor of
functional pipelines.
