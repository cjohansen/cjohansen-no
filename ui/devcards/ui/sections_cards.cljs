(ns ui.sections-cards
  (:require [dumdom.devcards :refer-macros [defcard]]
            [ui.elements :as e]
            [ui.highlight-cards :refer [CodeSample]]))

(defn centered-example []
  (e/centered
    {:title "Building static sites in Clojure with Stasis"
     :content [:div
               [:p [:a {:href "https://github.com/magnars/stasis/"} "Stasis"]
                " is a static site toolkit for Clojure. Unlike pretty much every other
static site generator though, it is not an \"opinionated framework\", or packed
full with flavor-of-the-month templating languages and whatnot. It is just a few
functions that helps with creating websites that can be hosted as static files,
and developed against a live server (enabling layouts, and various dynamic
features to generate the pages). As its Readme states; there are no batteries
included."]
               [:p "This post will take you through creating your first Stasis
site, and serving it with super-optimized frontend assets, courtesy of "
                [:a {:href "https://github.com/magnars/optimus/"} "Optimus"] ".
Both Stasis and Optimus are written by my good friend and colleague, "
                [:a {:href "http://emacsrocks.com/"} "Mr. Emacs Wizard"]
                ", " [:a {:href "https://github.com/magnars"} "Magnar Sveen."]]]}))

(defcard light-theme-1-centered-element
  [:div.theme-light1
   (centered-example)])

(defcard dark-theme-1-centered-element
  [:div.theme-dark1
   (centered-example)])

(defcard plain-section
  (e/section
    {:sub-title "Who is this for?"
     :content (list [:p "This post may interest anyone looking to set up a static site of some sort. The range of sites that can be successfully developed as static sites are bigger than you might think. While Stasis is a Clojure tool, only a very basic understanding of Lisp should be necessary to follow along."]
                    [:p "I'm hoping this post will not only show you the power of Clojure and Stasis for building static web sites, but also give you a good introduction to some very useful Clojure libraries. Maybe even to Clojure itself. In particular, I will discuss using these libraries: Stasis, Optimus, enlive, hiccup, cegdown, clygments and even write some tests with Midje."])}))

(def longer-section
  {:sub-title "Getting set up"
   :content (list
             [:p "First things first, let's get a project set up to serve our
               frontpage. If you've never worked with Clojure, install
               Leiningen. Now create your project:"]
             [CodeSample "<span></span>lein new cjohansen-no\n<span class=\"nb\">cd</span> cjohansen-no"]
             [:p "That creates an empty project for you. I will use this post to start the new code base for my blog. You might want to give yours a different name. Open project.clj, and add Stasis as a dependency. While you're at it, add a description and tune the license to your desires. When you're done, it should look something like this:"]
             [CodeSample "<span></span><span class=\"p\">(</span><span class=\"kd\">defproject </span><span class=\"nv\">cjohansen-no</span> <span class=\"s\">&quot;0.1.0-SNAPSHOT&quot;</span>\n  <span class=\"ss\">:description</span> <span class=\"s\">&quot;cjohansen.no source code&quot;</span>\n  <span class=\"ss\">:url</span> <span class=\"s\">&quot;http://cjohansen.no&quot;</span>\n  <span class=\"ss\">:license</span> <span class=\"p\">{</span><span class=\"ss\">:name</span> <span class=\"s\">&quot;BSD 2 Clause&quot;</span>\n            <span class=\"ss\">:url</span> <span class=\"s\">&quot;http://opensource.org/licenses/BSD-2-Clause&quot;</span><span class=\"p\">}</span>\n  <span class=\"ss\">:dependencies</span> <span class=\"p\">[[</span><span class=\"nv\">org.clojure/clojure</span> <span class=\"s\">&quot;1.5.1&quot;</span><span class=\"p\">]</span>\n                 <span class=\"p\">[</span><span class=\"nv\">stasis</span> <span class=\"s\">&quot;1.0.0&quot;</span><span class=\"p\">]])</span>\n"]
             [:p [:code "#\"...\""] " is Clojure syntax for regular expressions."])})

(defcard section-with-code-light1
  [:div.theme-light1
   (e/section longer-section)])

(defcard section-with-code-default
  [:div
   (e/section longer-section)])

(defcard section-with-code-dark1
  [:div.theme-dark1
   (e/section longer-section)])

(defcard image-left-section
  (e/section-media-front
   {:content (list (e/h2 {} "Bottling")
                   [:p "After a few days, the beer was at 1.014, so I cold
                   crashed it with gelatine and let it sit for a few days."]
                   [:p "After bottling the beer was refreshing and nice, but the
                   Nelson hops where much too noticable. The goal with this beer
                   was to have a relatively neutral beer on hand for guests, and
                   the Nelson made it a special interest beer. Luckily, after
                   quite some weeks, it is just as uninteresting as intended,
                   and I can hand it out to anyone."])
    :media [:img.img {:src "/devcard_images/IMG_1625.jpg"}]}))

(defcard image-right-section
  (e/section-media-back
   {:content (list (e/h2 {} "Bottling")
                   [:p "After a few days, the beer was at 1.014, so I cold
                   crashed it with gelatine and let it sit for a few days."]
                   [:p "After bottling the beer was refreshing and nice, but the
                   Nelson hops where much too noticable. The goal with this beer
                   was to have a relatively neutral beer on hand for guests, and
                   the Nelson made it a special interest beer. Luckily, after
                   quite some weeks, it is just as uninteresting as intended,
                   and I can hand it out to anyone."])
    :media [:img.img {:src "/devcard_images/IMG_1625.jpg"}]}))

(defcard image-wide-section
  (e/section-media
   {:size :mega-wide
    :media [:img.img {:src "/devcard_images/IMG_1603.jpg"}]}))

(defcard image-title-section
  (e/section-media
   {:size :mega-wide
    :media [:img.img {:src "/devcard_images/sepia.jpg"}]
    :title "Flour Water Salt Yeast 50% Whole Wheat"}))

(defcard wide-image-section
  (e/section-media
   {:size :wide
    :media [:img.img {:src "/devcard_images/IMG_1603.jpg"}]}))

(defcard normal-image-only-section
  (e/section-media
   {:media [:img.img {:src "/devcard_images/IMG_1603.jpg"}]}))

(defcard teaser-section
  (e/teaser-section
   {:title "More breads"
    :teasers [{:media [:img.img {:src "/devcard_images/IMG_1555.jpg"}]
               :title "Variations on Flour Water Salt Yeast 50% Whole Wheat"
               :published "September 1st 2019"
               :url "/fermentations/2019-09-01-fwsy-5050/"}
              {:media [:img.img {:src "/devcard_images/IMG_1603.jpg"}]
               :title "Flour Water Salt Yeast 50% Whole Wheat"
               :published "August 31st 2019"
               :url "/fermentations/2019-08-31-fwsy-5050/"}
              {:media [:img.img {:src "/devcard_images/P8240257.jpg"}]
               :title "Hamelman's Five Grain Bread with Paté Fermentèe"
               :published "August 21st 2019"
               :url "/fermentations/2019-08-31-fwsy-5050/"}]}))

(defcard teaser-section-no-images
  (e/teaser-section
   {:title "More Clojure"
    :teasers [{:title "Clojure in Production with tools.deps"
               :published "June 3rd 2019"
               :url "/clojure-in-production-tools-deps/"}
              {:title "tools.deps, figwheel-main, Devcards, and Emacs"
               :published "May 14th 2019"
               :url "/tools-deps-figwheel-main-devcards-emacs/"}
              {:title "Optimized Optimus Asset Paths from ClojureScript"
               :published "February 18th 2019"
               :url "/optimized-optimus-asset-paths-clojurescript/"}
              {:title "A Unified Specification"
               :published "January 3rd 2019"
               :url "/a-unified-specification/"}]}))

(defcard teaser-section-no-images
  (e/teaser-section
   {:title "Blog Posts"
    :teasers [{:title "AWS Free Tier"
               :published "July 24 2019"
               :url "/clojure-in-production-tools-deps/"
               :description "Dette er en liten historie om hvordan EFS nærmest tok livet av Kubernetes-clusteret vårt, og hvordan du kan unngå å havne i samme knipa — selvom du ikke bruker EFS."
               :kind :article}
              {:title "tools.deps, figwheel-main, Devcards, and Emacs"
               :published "May 14th 2019"
               :url "/tools-deps-figwheel-main-devcards-emacs/"
               :description "A primer on setting up a ClojureScript project with tools.deps, figwheel.main, devcards, and optionally Emacs."
               :kind :article}
              {:title "Optimized Optimus Asset Paths from ClojureScript"
               :published "February 18th 2019"
               :url "/optimized-optimus-asset-paths-clojurescript/"
               :kind :article}
              {:title "A Unified Specification"
               :published "January 3rd 2019"
               :url "/a-unified-specification/"
               :kind :article}]}))
