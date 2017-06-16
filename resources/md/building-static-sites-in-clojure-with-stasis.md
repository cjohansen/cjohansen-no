# Building static sites in Clojure with Stasis

[Stasis](https://github.com/magnars/stasis) is a static site toolkit for
Clojure. Unlike pretty much every other static site generator, though, it is not
an "opinionated framework", or packed full with flavor-of-the-month templating
languages and whatnot. It is just a few functions that helps with creating
websites that can be hosted as static files, and developed against a live server
(enabling layouts, and various dynamic features to generate the pages). As its
Readme states; there are no batteries included. This post will take you through
creating your first Stasis site, and serving it with super-optimized frontend
assets, courtesy of [Optimus](https://github.com/magnars/optimus). Both Stasis
and Optimus are written by my good friend and colleague,
[Mr. Emacs Wizard](http://emacsrocks.com),
[Magnar Sveen](https://github.com/magnars).

The source code for this post can be found
[on GitHub](https://github.com/cjohansen/cjohansen-no/tree/blog-post). As I plan
to evolve the code to become my new blog (eventually), you should look for the
`blog-post` tag, which represents the code as per this post.

## Who is this for?

This post may interest anyone looking to set up a static site of some sort. The
range of sites that can be successfully developed as static sites are bigger
than you might think. While Stasis is a Clojure tool, only a very basic
understanding of Lisp should be necessary to follow along.

I'm hoping this post will not only show you the power of Clojure and Stasis for
building static web sites, but also give you a good introduction to some very
useful Clojure libraries. Maybe even to Clojure itself. In particular, I will
discuss using these libraries: [Stasis](http://github.com/magnars/stasis),
[Optimus](http://github.com/magnars/optimus),
[enlive](https://github.com/cgrand/enlive),
[hiccup](https://github.com/weavejester/hiccup),
[cegdown](https://github.com/Raynes/cegdown),
[clygments](https://github.com/bfontaine/clygments) and even write some tests
with [Midje](https://github.com/marick/Midje).

<div class="toc" id="toc"></div>

## Getting set up
<a name="setup"></a>

First things first, let's get a project set up to serve our frontpage. If you've
never worked with Clojure, [install Leiningen](http://leiningen.org/). Now
create your project:

```sh
lein new cjohansen-no
cd cjohansen-no
```

That creates an empty project for you. I will use this post to start the new
code base for my blog. You might want to give yours a different name. Open
project.clj, and add Stasis as a dependency. While you're at it, add a
description and tune the license to your desires. When you're done, it should
look something like this:

```clj
(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]])
```

Now add a truly static page (i.e. a file). Create and open
src/cjohansen_no/web.clj. This is the namespace we will use to define our pages.
Put the following content in it:

```clj
(ns cjohansen-no.web
  (:require [stasis.core :as stasis]))

(defn get-pages []
  (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$"))
```

`#"..."` is Clojure syntax for regular expressions.

(Note that the namespace is called cjohansen-no, while the directory was called
cjohansen_no. If you misspell either of these, Clojure will bark at you with a
rather unfriendly message saying it can't find the namespace. Remember to double
check file/directory names and namespaces).

The web.clj file pulls in stasis under the local name `stasis`, and
defines one function. Stasis will expect to receive a map of `url =>
content` defining pages, so this is what `get-pages` does. The
content can be a function, in which case it will be only be called when Stasis
needs to serve this particular page. This lazy loading becomes useful if your
site is big enough for it to become slow by loading everything in one go. For
now, we'll not worry about it.

`slurp-directory` is a Stasis function that creates a map of files in
a directory (recursively). Every file in the subtree that matches the provided
regular expression (i.e. every html, css and js file under resources/public)
will be included. The keys in the map (e.g. the URLs), will be the file path
relative to `resources/public`. For example,
`resources/public/index.html` will be served as
`/index.html`.

Put the following in resources/public/index.html:

```html
&lt;!DOCTYPE html&gt;
&lt;html&gt;
  &lt;head&gt;
    &lt;title&gt;My blog&lt;/title&gt;
  &lt;/head&gt;
  &lt;body&gt;
    &lt;h1&gt;My blog&lt;/h1&gt;
    &lt;p&gt;
      Welcome to it.
    &lt;/p&gt;
  &lt;/body&gt;
&lt;/html&gt;
```

As Stasis is a "no batteries included" framework, we need to do some work to
either export the static site or view it live through a web server. As the file
is already static, we'll set up the live server first. Update project.clj to
pull in a couple of dependencies and configure a Leiningen plugin:

```clj
(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]]
  :ring {:handler cjohansen-no.web/app}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})
```

[Ring](https://github.com/ring-clojure/ring) is the defacto HTTP toolkit for
Clojure, and can be compared to Python's WSGI or Ruby's Rack. We have added it
as a dependency, and set up the `app` function in the
`cjohansen-no.web` namespace (e.g. the file we created earlier) to be
the entry-point for the web server. The profile configuration loads some
convenient Leiningen tasks for the development profile (which is also the
default profile).

A web application in Ring is just a function that receives as its only argument
a request hash and returns a map like the following:

```clj
{:status 200
 :headers {"Content-Type" "text/html"}
 :body "Hello World"}
```

Stasis provides the `serve-pages` function to help us produce this
response. It returns a function that will look at the request and find the
corresponding page in our map. Add the `app` function to the
src/cjohansen-no/web.clj file:

```clj
(def app (stasis/serve-pages get-pages))

Run the server:

```sh
>lein ring server
```

This will pop up a browser displaying your static HTML file in all its naked
glory.

## Adding templating
<a name="templating"></a>

Next we will add a page that is split between the content/body of the page and
the wrapping layout. The layout will be shared by many files, so applying it in
one central place saves us some work.

Add a partial page to resources/partials/about.html

```html
&lt;h1&gt;About this site&lt;/h1&gt;
&lt;p&gt;
  It is a web page.
&lt;/p&gt;
```

Rather than using HTML manually to create the layout, we will use the popular
Clojure templating library called
[Hiccup](https://github.com/weavejester/hiccup). Hiccup allows us to express
HTML in a more compact form by using vectors, keywords and maps. It is best
illustrated with an example. Add Hiccup to project.clj:

```clj
:dependencies [[org.clojure/clojure "1.5.1"]
               [stasis "1.0.0"]
               [ring "1.2.1"]
               [hiccup "1.0.5"]] ;; Like so
```

Alter the namespace form in src/cjohansen_no/web.clj to require the
`html5` function from Hiccup:

```clj
(ns cjohansen-no.web
  (:require [hiccup.page :refer [html5]]
            [stasis.core :as stasis]))
```

Add the following function to the same file:

```clj
(defn layout-page [page]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:title "Tech blog"]
    [:link {:rel "stylesheet" :href "/styles/styles.css"}]]
   [:body
    [:div.logo "cjohansen.no"]
    [:div.body page]]))
```

As you can see, Hiccup markup is mostly a leaner version of HTML. One thing that
makes Hiccup very cool is that is accepts elements like the ones in our example,
nested lists of elements, and nils. This means that we can map over data
structures inline in the Hiccup structure without having to worry about nested
lists creating nested markup structures. Think of lists as the
[DocumentFragments](https://developer.mozilla.org/en/docs/Web/API/DocumentFragment)
of Hiccup. We can also inline `if` forms without else forms without
worrying about dangling `nil`s causing weird artefacts in the
generated markup.

To use the new layout, we will add a page definition for our partial page, and
apply the layout on it. To read files from the app's resources directory,
require the `clojure.java.io` package:

```clj
(ns cjohansen-no.web
  (:require [clojure.java.io :as io]
            [hiccup.page :refer [html5]]
            [stasis.core :as stasis]))
```

Add the about page function:

```clj
(defn about-page [request]
  (layout-page (slurp (io/resource "partials/about.html"))))
```

Finally, add the new page to the page map created in `get-pages`:

```clj
(defn get-pages []
  (merge (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
         {"/about/" about-page}))
```

Because we added a new dependency to project.clj, you now need to quit the
server (Ctrl+C) and restart it. When you have, /about/ should present you with
your fantastic new page-with-layout.

### Serving all partials

Doing all this work for every new partial page seems a tad bit too laborious. We
will generalize the code we just added by re-writing it to serve all partial
pages under `resources/partials` in the same way.

Remember how `(stasis/slurp-directory "resources/partials"
#".*\.html$")` will create a map with relative paths as keys and the
contents of the corresponding files as values? This is almost what we want,
except we now also want to wrap the content in the layout. To achieve this, we
will loop through the map and wrap all the values in a function call that adds
the layout. Like so:

```clj
(defn partial-pages [pages]
  (zipmap (keys pages)
          (map layout-page (vals pages))))
```

This function accepts as input the map produced by Stasis'
`slurp-directory` and returns a map where the values have all been
wrapped in a layout. The `zipmap` function takes two collections and
returns a map. It builds each map entry by pulling an entry from the first
collection to use as the key, and an entry from the second collection to use as
the value. Now remove the `about-page` function, and update
`get-pages` to look like this:

```clj
(defn get-pages []
  (merge (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
         (partial-pages (stasis/slurp-directory "resources/partials" #".*\.html$"))))
```

This will *almost* work. You will find our about page from before at
[/about.html](http://localhost:3000/about.html), instead of /about. The reason
is that `slurp-directory` uses the relative file names as paths. We can fix this
in one of two ways:

1. Mapping the keys as well, to lose the extension. While this will certainly
   work, it will produce odd results for any file called index.html

2. Rename `resources/partials/about.html` to
   `resources/partials/about/index.html`

We will opt for option 2 in this case. I will show option 1 when we deal with
markdown files.

### Path conflicts

Now that we have two page sources, and both of them create root-level URLs,
there is a risk that we end up with conflicts. With our current use of
`merge`, any page in `resources/partials` will silently
shadow a page in `resources/public` with the same path. Update
`get-pages` to the following to avoid the problem:

```clj
(defn get-pages []
  (stasis/merge-page-sources
   {:public (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
    :partials (partial-pages (stasis/slurp-directory "resources/partials" #".*\.html$"))}))
```

The `merge-page-sources` function works pretty much like
`merge`, except it will throw an exception if either source defines
duplicate URLs. The map keys are useful to determine the source of the conflict.
For instance, if you were to add `public/about/index.html`, you would
get this error:

```text
URL conflicts between :public and :partials: #{"/about/index.html"}
```

(That last bit is Clojure set notation by the way).

## Writing in markdown
<a name="markdown"></a>

Partial pages are nice, but being able to write in markdown would be even
better. [Cegdown](https://clojars.org/me.raynes/cegdown) is a Clojure wrapper
for Pegdown, a popular Java library for rendering markdown. Add it to your
project.clj:

```clj
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]] ; Like so
```

Now, add it to the namespace form in src/cjohansen_no/web.clj. While we're at
it, we will require the Clojure string library as well (we'll use it shortly):

```clj
(ns cjohansen-no.web
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]))
```

Now we will add a function to render every page in `resources/md` as
markdown. It will be very similar to the partials we did before, but now with
markdown rendering as well. Because we don't want ".md" as part of the URL for
these pages, we will map the keys as well.

```clj
(defn markdown-pages [pages]
  (zipmap (map #(str/replace % #"\.md$" "") (keys pages))
          (map #(layout-page (md/to-html %)) (vals pages))))
```

The `#( )` form is a function literal. Inside it, `%`
refers to the first argument. In the above example, the following are identical:

```clj
#(str/replace % #"\.md$" "")
;; ...and:
(fn [path] (str/replace path #"\.md$" ""))
```

I wrote more on the anonymous function literal in
[a separate post](/clojure-to-die-for/).

The final step is to add the new page source to our map:

```clj
(defn get-pages []
  (stasis/merge-page-sources
   {:public
    (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
    :partials
    (partial-pages (stasis/slurp-directory "resources/partials" #".*\.html$"))
    :markdown
    (markdown-pages (stasis/slurp-directory "resources/md" #"\.md$"))}))
```

Add the following to `resources/md/my-first-post.md`:

```text
# My first post

It's pretty short for now.
```

Restart the server again (we added more new dependencies, remember?) When you
have, /my-first-post should present you with your brief, but lovely blog post.

## Post-processing: Syntax highlighting
<a name="syntax-highlighting"></a>

Any self-respecting tech blog needs nice syntax highlighting for code blocks.
When it comes to syntax highlighting, [Pygments](http://pygments.org/) is the
bee's knees. It supports just about any language you can think of, there's a
bunch of color themes around and it is stable and resillient. It is also the
library used to highlight code on GitHub.
[Clygments](https://github.com/bfontaine/clygments) is a Clojure interface to it
(which uses Jython; Pygments is a Python library).

We will add syntax highlighting as a post-processing step for HTML. This way, we
can support syntax highlighting for full static pages in the public directory,
partial pages and pages rendered from markdown. To do this, we will use another
templating library for Clojure, [enlive](https://github.com/cgrand/enlive).
Actually, enlive is more than just a templating library. As you will see, it can
be used to transform documents in various interesting ways.

### A code block

Let's add a
[fenced code block](https://help.github.com/articles/github-flavored-markdown#fenced-code-blocks)
to our markdown file:

```text
# My first post

It's pretty short for now. Here's our project.clj:

```clj
(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]]
  :ring {:handler cjohansen-no.web/app}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})
```

In order for this work, we need to inform `cegdown` that we want to
enable the fenced code blocks extension. While we're at it, we'll enable a
couple of other useful extensions as well:

```clj
(def pegdown-options ;; https://github.com/sirthias/pegdown
  [:autolinks :fenced-code-blocks :strikethrough])

(defn render-markdown-page [page]
  (layout-page (md/to-html page pegdown-options)))

(defn markdown-pages [pages]
  (zipmap (map #(str/replace % #"\.md$" "") (keys pages))
          (map render-markdown-page (vals pages))))
```

Reloading the blog post will show you how the fenced code blocks are rendered:

```html
&lt;pre&gt;&lt;code class="clj"&gt;...&lt;/code&gt;&lt;/pre&gt;
```

We will now use enlive to extract this piece of markup and replace it with the
version highlighted by clygments. First off, add the new dependencies to
project.clj (remember to restart the server!)

```clj
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]
                 [enlive "1.1.5"] ; New
                 [clygments "0.1.1"]] ; New
```

Add a new namespace (i.e., file) to the project. Copy the following code into
`src/cjohansen_no/highlight.clj`:

```clj
(ns cjohansen-no.highlight
  (:require [clojure.java.io :as io]
            [clygments.core :as pygments]
            [net.cgrand.enlive-html :as enlive]))
```

Enlive has many tricks up its sleave. Perhaps the most interesting one is the
somewhat confusingly named `sniptest`. It takes some HTML as a
string, and selector/function pairs. It will then;

1. Parse the HTML
2. Find all nodes matching the selector
3. Call the corresponding function once for every match
4. Replace the node with the result of calling the function
5. Return the transformed HTML as a string

```clj
(defn highlight-code-blocks [page]
  (enlive/sniptest page
            [:pre :code] highlight
            [:pre :code] #(assoc-in % [:attrs :class] "codehilite")))
```

This function will find every `code` element inside a
`pre` element and pass it through the `highlight` function
(to be shown). Then, it will add a class name to the same elements. In practice,
you would probably do both in the `highlight` function, but this
allows me to illustrate how you can perform multiple transformations in one go.
The "codehilite" class name just happens to be class name used by the Pygments
CSS themes available [here](https://github.com/richleland/pygments-css.git) (we
will include this later). Add the second function:

```clj
(defn- highlight [node]
  (let [code (->> node :content (apply str))
        lang (->> node :attrs :class keyword)]
    (pygments/highlight code lang :html)))
```

The dash in `defn-` means that this function is private, and only
referrable within the current namespace. The nodes that enlive operate on are
maps like this:

```clj
{:tag :code
 :attrs {:class "clj"}
 :content [...]}
```

The content is a list of new nodes and/or strings.

`-&gt;&gt;` is the
[thread-last macro](http://clojuredocs.org/clojure_core/clojure.core/-%3E%3E).
It takes any number of arguments, threads values from left to right; given
`(-&gt;&gt; a (b 1 2) c)`, it will take the value `a`,
pass it as the last argument to `b` (i.e. `(b 1 2 a)`),
pass the return value of that expression as the last argument to `c`,
and finally return the result of that. So the above line:

```clj
(->> node :content (apply str))
```

Is the same as this:

```clj
(apply str (:content node))
```

I wrote more on threading macros in [a separate post](/clojure-to-die-for/).

Using a keyword as a function is one way to look up that key in a map. The apply
call means to call str with the following list of arguments, as individual
arguments, not one list (e.g. `(fn a b c)`, not `(fn
[a b c])`).

To preview the highlighting in the browser, we need to make some changes to the
web namespace. Start by updating the namespace form to pull in our new
dependency:

```clj
(ns cjohansen-no.web
  (:require [cjohansen-no.highlight :refer [highlight-code-blocks]] ; This one
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]))
```

Syntax highlighting should apply to all pages. A good place to do it is between
our old `get-pages` function and Stasis' rendering. We will do this
by adding a `prepare-pages` function:

```clj
(defn prepare-pages [pages]
  (zipmap (keys pages)
          (map #(highlight-code-blocks %) (vals pages))))
```

Again, we use zipmap to produce a new map where the keys are untouched, but the
values have been mapped. To make Stasis run through this, rename
`get-pages` to `get-raw-pages`, and add a new get-pages:

```clj
(defn get-raw-pages []
  (stasis/merge-page-sources
   {:public
    (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
    :partials
    (partial-pages (stasis/slurp-directory "resources/partials" #".*\.html$"))
    :markdown
    (markdown-pages (stasis/slurp-directory "resources/md" #"\.md$"))}))

(defn prepare-pages [pages]
  (zipmap (keys pages)
          (map #(fn [req] (highlight-code-blocks %)) (vals pages))))

(defn get-pages []
  (prepare-pages (get-raw-pages)))
```

Reloading the markdown page should show you that what we've done so far both
kinda worked and kinda didn't.

We are getting highlighted code. However, Pygments includes some unwanted
wrapping markup. As we're already inside a `pre` element, the result
is not quite as desired. To fix this we will use enlive once more to massage the
output from Pygments. The output includes a wrapper div and a pre, let's extract
just the code:

```clj
(defn- extract-code
  [highlighted]
  (-> highlighted
      java.io.StringReader.
      enlive/html-resource
      (enlive/select [:pre])
      first
      :content))

(defn- highlight [node]
  (let [code (->> node :content (apply str))
        lang (->> node :attrs :class keyword)]
    (assoc node :content (-> code
                             (pygments/highlight lang :html)
                             extract-code))))

```

Enlive's `select` function selects elements from a document, but
unlike `sniptest`, it does not accept a string. Instead, we must go
through its `html-resource` function, which only accepts input
streams. The end result is that we do a select on what Pygments gives us in
order to get just the highlighted code. Refreshing the blog post shows that it
works as expected.

The `-&gt;` is the thread-first macro. It works like thread-last,
except it threads values as the first argument to the next function. The above
example could be written in either of these ways, but most people find the
threading form to be the easiest on the eyes:

```clj
(-> highlighted
    java.io.StringReader.
    enlive/html-resource
    (enlive/select [:pre])
    first
    :content)

;; Same as:
(:content (first (enlive/select (enlive/html-resource
                                  (java.io.StringReader. highlighted))
                                [:pre])))

```

To add some styling, pick a CSS file from
[the suggested themes repo](https://github.com/richleland/pygments-css.git), and
load it onto the page. Update the `layout-page` in web.clj to look
like this:

```html
(defn layout-page [request page]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:title "Tech blog"]
    [:link {:rel "stylesheet" :href "/pygments-css/autumn.css"}]]
   [:body
    [:div.logo "cjohansen.no"]
    [:div.body page]]))
```

## Lazy pages
<a name="lazy"></a>

As we're adding more features to our site, it is becoming apparent that
processing all the pages to completion on every request isn't ideal. Fixing this
is quite easy with Stasis, because we can give Stasis functions instead of
strings, and then Stasis will call the function to build a particular page only
when it needs to render that specific page.

To make our pages lazy, update `prepare-pages` to replace the values
with functions instead of strings of highlighted HTML. The function should take
one argument, the request map.

```clj
(defn prepare-pages [pages]
  (zipmap (keys pages)
          (map #(fn [req] (highlight-code-blocks %)) (vals pages))))
```

By having the function literal return a new function that takes one argument, we
have significantly improved performance for our development server.

## Asset optimization
<a name="assets"></a>

Now that we have a blog with syntax highlighting, we need to start thinking
about delivery. Fast webpages beat slow ones on all sorts of metrics. One way to
make our site faster is by employing various frontend asset optimization
techniques. For this purpose, there is (among others)
[Optimus](https://github.com/magnars/optimus). We will use it to:

* Concatenate CSS and JavaScript files
* Minify CSS and JavaScript files
* Serve CSS and JavaScript from cache-friendly URLs

A "cache friendly" URL is one that is unique every time the contents of the URL
changes. This way we can serve assets with aggressive cache headers, and users
will only need to download them once. The next time we deploy, if the assets
have changed, they will have a new URL. Optimus facilitates this by providing
some functions to help us link to assets.

We will start by adding Optimus as a dependency in project.clj. Remember to
restart the server after doing this.

```clj
(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]
                 [enlive "1.1.5"]
                 [clygments "0.1.1"]
                 [optimus "0.14.2"]] ; New
  :ring {:handler cjohansen-no.web/app}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})

```

Now update the web.clj namespace form to require some functions from Optimus:

```clj
(ns cjohansen-no.web
  (:require [optimus.assets :as assets]                     ; New
            [optimus.optimizations :as optimizations]       ; New
            [optimus.prime :as optimus]                     ; New
            [optimus.strategies :refer [serve-live-assets]] ; New
            [cjohansen-no.highlight :refer [highlight-code-blocks]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]))
```

Instead of having Stasis serve the files in public, we will hand them to Optimus
as assets. We will define a separate function for these assets, as it makes for
a natural place to add further assets and/or bundles of assets later:

```clj
(defn get-assets []
  (assets/load-assets "public" [#".*"]))
```

If your CSS files use `@import`, Optimus will (by default) take care
to inline the import, so there is no need to define bundles at this point. Refer
to the [Optimus readme](https://github.com/magnars/optimus#readme) for more
details.

To make our app use the new assets, we will change the `app`
function:

```clj
(def app
  (optimus/wrap (stasis/serve-pages get-pages)
                get-assets
                optimizations/all
                serve-live-assets))
```

The call to `stasis/serve-pages` returns a function (a Ring app,
remember?) `optimus/wrap` returns another function with the same
signature that wraps the original one. We pass it the function to get all our
assets, optimization rules (a function) and a strategy for serving the assets
(also a function). `optimizations/all` is a grab bag of every trick
Optimus knows:

* Minify JavaScript
* Minify CSS
* Inline CSS imports
* Concatenate bundles
* Add cache-bust expires headers (replace URL references with generated unique ones)
* Add last-modified headers

You are free to pick and choose from this list if you want, but for most cases,
`optimizations/all` is what you want.

Lastly, we employed Optimus' `serve-live-assets` strategy, which
means that Optimus will read assets from disk on every request. This is useful
in development mode, but in a production setting, you would typically use one
that's less resource intensive, like `serve-frozen-assets`.

Create a CSS file and make sure it gets included from the page layout.

```css
import url(../pygments-css/autumn.css);

body {
    font: 16px Helvetica, arial, freesans, clean, sans-serif;
    line-height: 1.5;
    margin: 0 10px;
}
```

Refreshing the blog in the browser should display the same page as before.
However, if you hit the CSS file directly, you will find that Optimus has done
what it can to optimize serving it.

### Rewriting links

There is one final thing to take care of. In production, we can configure our
web server to serve assets with a far future expires header. But in order for
that to be safe, we need distinct URLs for every change to the file. Let's add
another require to the web namespace form:

```clj
(ns cjohansen-no.web
  (:require [optimus.assets :as assets]
            [optimus.link :as link] ; New
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [cjohansen-no.highlight :refer [highlight-code-blocks]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]))
```

With this in place, we can use Optimus to generate the link to the CSS file.
However, to do that, it needs access to the request map, so we need to change a
few things. We will start with the `layout-page` function:

```clj
(defn layout-page [request page]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:title "Tech blog"]
    [:link {:rel "stylesheet" :href (link/file-path request "/styles/main.css")}]]
   [:body
    [:div.logo "cjohansen.no"]
    [:div.body page]]))
```

Both the `partial-pages` and `markdown-pages` need to pass
the request to `layout-page`. If we change them to return functions,
Stasis will call those functions with the request.

```clj
(defn partial-pages [pages]
  (zipmap (keys pages)
          (map #(fn [req] (layout-page req %)) (vals pages))))
```

Remember that `#( )` is a function literal, so the mapping function
here is a function that returns another function (which takes a request map as
its only argument). The markdown generation is similar, but includes the
additional step of running the content through cegdown:

```clj
(defn markdown-pages [pages]
  (zipmap (map #(str/replace % #"\.md$" "") (keys pages))
          (map #(fn [req] (layout-page req (md/to-html % pegdown-options)))
               (vals pages))))
```

Previously these maps contained strings, so we need to update their use now that
they're functions. We start with a new function:

```clj
(defn prepare-page [page req]
  (-> (if (string? page) page (page req))
      highlight-code-blocks))
```

This function takes a page and a request. Because every page will go through
this function, some will be strings, and some will be functions. If the page is
a string, we leave it untouched, and if it's a function, we call it with the
request map and pipe the result through a series of post-processing steps.
There's currently only one step, but the threading macro has set us up for
easily adding more steps later. The final piece of the puzzle is to update the
`prepare-pages` function:

```clj
(defn prepare-pages [pages]
  (zipmap (keys pages)
          (map #(partial prepare-page %) (vals pages))))
```

Again, we use the function literal `#( )`. We also use
`partial`. This returns a new function that knows the first argument
to pass to `prepare-page`. When you call this new function with one
argument (a request), the `prepare-page` function will be called with
a page and a request. Update the page in the browser, view source and note that
Optimus has now given our CSS file a nice and unique URL.

## Export to disk
<a name="exports"></a>

So far we've only surfed the server version, but the whole point of this
exercise was to create something that can work as a static site. To dump the
file to disk, start by adding a custom Leiningen build alias in project.clj:

```clj
(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]
                 [enlive "1.1.5"]
                 [clygments "0.1.1"]
                 [optimus "0.14.2"]]
  :ring {:handler cjohansen-no.web/app}
  :aliases {"build-site" ["run" "-m" "cjohansen-no.web/export"]} ; New
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})
```

This configures `lein build-site` as a command that will invoke the
`export` function in the `cjohansen-no.web` namespace.
Stasis gives us what we need to build this function:

```clj
(def export-dir "dist")

(defn export []
  (stasis/empty-directory! export-dir)
  (stasis/export-pages (get-pages) export-dir))
```

While this won't technically fail, it also won't be the whole picture. Had we
not been using Optimus, this would be OK. Since we are using Optimus, we want to
make sure the export is optimized as well. The fix is simple; tell Optimus to
dump assets for us, and add an entry to Stasis' request map extensions so that
Optimus finds the assets. First update the namespace form to require the Optimus
`export` library:

```clj
(ns cjohansen-no.web
  (:require [optimus.assets :as assets]
            [optimus.export]            ; New
            [optimus.link :as link]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.strategies :refer [serve-live-assets]]
            [cjohansen-no.highlight :refer [highlight-code-blocks]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]))
```

Then update the export function:

```clj
(defn export []
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets})))
```

Now, on the command line, run `lein build-site`. After a short while
you will find your entire site ready to ship in the `dist` directory.
This can be directly rsynced to your server.

## Testing and verification
<a name="testing"></a>

Building sites like we've done in this post opens for various interesting ways
of programatically performing tests and health checks. I will show you two
simple, yet immensely useful tests we can add to a site of this kind. You can of
course also add unit tests for individual functions, and doing so in a system
composed of mostly pure functions is very straight-forward, yet outside the
scope of this post.

### Testing for 200 OK

One nice test to put in a site like this is an integration test that checks that
every page renders without errors. We will use
[Midje](https://github.com/marick/Midje) for our tests, so let's update
project.clj:

```clj
(defproject cjohansen-no "0.1.0-SNAPSHOT"
  :description "cjohansen.no source code"
  :url "http://cjohansen.no"
  :license {:name "BSD 2 Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]
                 [enlive "1.1.5"]
                 [clygments "0.1.1"]
                 [optimus "0.14.2"]]
  :ring {:handler cjohansen-no.web/app}
  :aliases {"build-site" ["run" "-m" "cjohansen-no.web/export"]}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}
             :test {:dependencies [[midje "1.6.0"]]    ; New
                    :plugins [[lein-midje "3.1.3"]]}}) ; New

```

We've added a test profile that includes the midje dependencies. Add the
following to test/cjohansen_no/web_test.clj:

```clj
(ns cjohansen-no.web-test
  (:require [cjohansen-no.web :refer :all]
            [midje.sweet :refer :all]))

(fact
 "All pages respond with 200 OK"

 (doseq [url (keys (get-pages))]
   (let [status (:status (app {:uri url}))]
     [url status] => [url 200])))

```

We simply call our `get-pages` function, loop the resulting map, and
call each page function with a request map consisting only of a URL. The
comparison is made with a vector of the URL and the status. The reason for this
is that the URL will be included in the error message if this fails. This way we
can know which pages fail. To run the tests:

```sh
lein with-profile test midje
```

Doing this will inform us that the generated core_test.clj fails. Just delete
it. Other than that, the test confirms that all is well with our site. To keep
the tests running while working on the site, run autotest:

```sh
lein with-profile test midje :autotest
```

### Building a link checker with enlive

Another useful test to have in place is a link-checker. We will make one that at
least verifies that the internal links between pages in our app are correct, and
that they don't cause any unnecessary redirects (e.g. from /about to /about/).

Enlive is very useful for these things. We will use the `select`
function to find all links, and then make sure that the `href`
attribute points to an existing URL if it is a path (not a full URL, which is
treated as an external link). First up is the `link-valid?` function,
which checks if a single link is valid given a map of pages:

```clj
(defn link-valid? [pages link]
  (let [href (get-in link [:attrs :href])]
    (or
     (not (.startsWith href "/"))
     (contains? pages href)
     (contains? pages (str href "index.html")))))
```

The link is considered valid if the href attribute either points to a URL that
isn't a path within our app (relative paths are assumed not used) or if it
points to one of the pages in the map. We're lenient enough to allow links to
/about/ when we have /about/index.html. Since we will use enlive to select all
the links, update the test namespace form to this:

```clj
(ns cjohansen-no.web-test
  (:require [cjohansen-no.web :refer :all]
            [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as enlive]))
```

Then add the test itself:

```clj
(fact
 "All links are valid"

 (let [pages (get-pages)]
   (doseq [url (keys (get-pages))
           link (-> (:body (app {:uri url}))
                      java.io.StringReader.
                      enlive/html-resource
                      (enlive/select [:a]))]
     (let [href (get-in link [:attrs :href])]
       [url href (link-valid? pages link)] => [url href true]))))
```

Again, we loop all the pages and get them. For each page, we select all links,
and expect all of them to pass the link checker. Again we make a slightly
strange comparison in the interest of having more than true/false in the output
if one of these fail. If you add an invalid link to the markdown file now,
running the tests will produce this:

```text
FAIL "All links are valid" at (web_test.clj:21)
    Expected: ["/my-first-post" "/about/" true]
      Actual: ["/my-first-post" "/about/" false]
```

18 lines of code to verify all links on the site. Pretty nifty! Rather than
having numerous tests that load all the pages, it would probably be a good way
to change the structure of the tests such that we only load each page once, and
instead register various test functions we want to run for each page. This is
left as an exercise for the reader.

## Summary
<a name="summary"></a>

I hope this post has shown you the power and flexibility of Stasis and all the
other tools. Perhaps it has even convinced you further of the value of Clojure.
I really do dislike number-of-lines-of-code jerkoffs, but it is worth mentioning
that we were able to build a reasonably feature-complete technical blog in
roughly 100 lines of code using a simple, yet powerful "no batteries included"
library like Stasis (which itself clocks in at just over 100 lines of code). I
hope you will consider Clojure and Stasis for your next semi-static web project.

Big thanks to Magnar Sveen for proof-reading and correcting this post.

<div class="meta">
  <p class="twitter">[Follow me (@cjno) on Twitter](http://twitter.com/cjno)</p>
  <div class="contribute">
    <h2>Discuss</h2>
    <ul>
      <li class="hackernews">[Hacker News discussion](https://news.ycombinator.com/item?id=7375425)</li>
      <li class="reddit">[Reddit discussion](http://www.reddit.com/r/Clojure/comments/202qs2/building_static_sites_in_clojure_with_stasis/)</li>
    </ul>
  </div>
  <div id="tweets" class="comments"></div>
 </div>
