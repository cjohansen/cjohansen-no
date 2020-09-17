--------------------------------------------------------------------------------
:type :meta
:title Editing for cleaner merges
:published #time/ldt "2020-09-17T12:00"
:tags [:programming :git]
:description

As we add new code to existing files, it feels natural to append – add new
functions at the end of the file, new requires at the end of the list of
requires, and so on. This approach introduces some friction, and in this post
I'll share some pointers for improved editing and git workflow.

--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title Editing for cleaner merges
:body

As we add new code to existing files, it feels natural to append – add new
functions at the end of the file, new requires at the end of the list of
requires, and so on. This approach introduces some friction, and in this post
I'll share some pointers for improved editing and git workflow.

--------------------------------------------------------------------------------
:type :section
:title Merge conflicts, oh my!
:body

When code is mostly appended at the end of things, you will more frequently end
up with git merge conflicts. This problem is quite easy to illustrate.

Bob starts work on a wholly unnecessary maths library for Clojure:

```clj
(ns maths.core)

(defn add [a b]
  (+ a b))

(defn modulo [a b]
  (mod a b))
```

He commits and pushes this code. Sally clones the upstream repo, and adds a
`multiply` function:

```clj
(ns maths.core)

(defn add [a b]
  (+ a b))

(defn modulo [a b]
  (mod a b))

(defn multiply [a b]
  (* a b))
```

Meanwhile, Bob is hard at work on the `divide` function:

```clj
(ns maths.core)

(defn add [a b]
  (+ a b))

(defn modulo [a b]
  (mod a b))

(defn divide [a b]
  (/ a b))
```

Sally finishes, commits, and pushes. When Bob tries to push his addition, git
stops him. He pulls with a rebase, and BAM! Git presents him with a conflict:

```clj
(ns maths.core)

(defn add [a b]
  (+ a b))

(defn modulo [a b]
  (mod a b))

<<<<<<< HEAD
(defn multiply [a b]
  (* a b))
=======
(defn divide [a b]
  (/ a b))
>>>>>>> Add division
```

Because Sally and Bob both appended their code at the end of the file, the
unlucky one to push last is left to figure out how it all fits together.

--------------------------------------------------------------------------------
:type :section
:title A different tale
:body

In the second version of our tale, Sally is the instigator of the maths
library, and she decides that functions should not be added randomly at the
bottom of the file. Instead, functions should be added alphabetically.

She starts out with the same two functions from before:

```clj
(ns maths.core)

(defn add [a b]
  (+ a b))

(defn modulo [a b]
  (mod a b))
```

Bob clones his copy, and adds the `multiply` function, this time in the
alphabetically appropriate location:

```clj
(ns maths.core)

(defn add [a b]
  (+ a b))

(defn modulo [a b]
  (mod a b))

(defn multiply [a b]
  (* a b))
```

Sally adds the `divide` function in its alphabetically appropriate location:

```clj
(ns maths.core)

(defn add [a b]
  (+ a b))

(defn divide [a b]
  (/ a b))

(defn modulo [a b]
  (mod a b))
```

Because Bob pushed first, Sally must now fetch and rebase her changes on top of
those in the upstream repository. However, because Bob and Sally's code were
added in different places, git is able to automatically combine their additions.
Win!

--------------------------------------------------------------------------------
:type :section
:title Please alphabetize it
:body

By having some other strategy for adding new code than "append", Bob and Sally
were able to find a smoother workflow. Alphabetizing is one way to go, but other
strategies will probably work as well.

Alphabetizing appeals to me because it's objective. If your strategy is to
"group similar functions", different developers _will_ make different
associations. Alphabetizing your code both reduces git merge conflicts and makes
it trivially easy to know where to place new code. Because you know where to
place code, you also know roughly where to find it when reading. Win win win!

I should note that the language you work in may place some constraints that
defies alphabetization: in Clojure, functions must be declared before use. No
need to bend over backwards with `(declare fn-name)` – place the function as
close to the alphabetically ideal place while satisfying restraints in your
particular language.

I should also note that this does not completely remove merge conflicts, but it
does reduce their occurrence noticably.

--------------------------------------------------------------------------------
:type :section
:title Act locally, act globally
:body

Alphabetizing, or some other strategy than "append", works great locally within
a file as well. Whether it is a list of tags like this:

```clj
{:tag/aws "AWS"
 :tag/css "CSS"
 :tag/datomic "Datomic"
 :tag/emacs "Emacs"
 :tag/git "Git"
 :tag/kubernetes "Kubernetes"
 :tag/tools.deps "tools.deps"
 :tag/clojure "Clojure"}
```

Or the dependencies of a namespace like this:

```clj
(ns cjohansen-no.ingest
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]))
```

Alphabetizing vastly reduces the number of merge conflicts when multiple
developers work on the same codebase. Try it!
