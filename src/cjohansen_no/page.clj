(ns cjohansen-no.page
  (:require [cjohansen-no.html-walker :as html-walker]
            [clojure.java.io :as io]
            [cjohansen-no.images :as images]
            [clojure.string :as str]
            [clygments.core :as pygments]
            [dumdom.string :as dumdom]
            [glow.core :as glow]
            [imagine.core :as imagine]
            [optimus.link :as link]))

(defn- extract-code [highlighted]
  (.getInnerHTML (first (html-walker/find highlighted [:pre]))))

(defn highlight-code-str [lang code]
  (if (#{:clj :cljs :clojure :clojurescript :edn} lang)
    (glow/highlight-html code)
    (pygments/highlight code (or lang "text") :html)))

(defn highlight [^ch.digitalfondue.jfiveparse.Node node]
  (let [lang (some-> node (.getAttribute "class") not-empty (str/replace #"language-" "") keyword)
        pygments-info (some-> node (.getAttribute "data-pygments"))
        code (-> (.getInnerHTML node)
                 (str/replace "&lt;" "<")
                 (str/replace "&gt;" ">"))]
    ;; Certain code samples (like a 14Kb HTML string embedded in JSON) trips up
    ;; Pygments (too much recursion). When that happens, skip highlighting
    (try
      (.setInnerHTML node (->> code (highlight-code-str lang) extract-code))
      (catch Exception e
        (println "Failed syntax highlighting")
        (println (.getMessage e))))))

(def skip-pygments?
  (= (System/getProperty "cjohansen.skip.pygments") "true"))

(defn maybe-highlight [^ch.digitalfondue.jfiveparse.Node node]
  (when-not (or skip-pygments? (= "ignore" (some-> node (.getAttribute "data-pygments"))))
    (highlight (.getFirstChild node))
    (.setAttribute node "class" "codehilite")))

(def norwegian-char-replacements
  {"æ" "e"
   "ø" "o"
   "å" "a"
   "Æ" "E"
   "Ø" "O"
   "Å" "A"})

(defn to-id-str [str]
  (-> (str/lower-case str)
      (str/replace #"[æøåÆØÅ]" norwegian-char-replacements)
      (str/replace #"[^a-zA-Z0-9]+" "-")
      (str/replace #"-$" "")
      (str/replace #"^-" "")))

(defn- add-anchor [node]
  (when-not (= "a" (.getNodeName (first (.getChildNodes node))))
    (let [id-str (to-id-str (.getTextContent node))]
      (.setInnerHTML
       node
       (dumdom/render
        [:a.anchor-link {:id id-str :href (str "#" id-str)}
         [:span.anchor-marker "¶"]
         (.getInnerHTML node)])))))

(defn- optimize-path-fn [req]
  (fn [src]
    (let [[url skigard] (str/split src #"#")]
      (str
       (or (not-empty (link/file-path req url))
           (imagine/realize-url images/image-asset-config url)
           (throw (Exception. (str "Image not loaded: " url))))
       (some->> skigard (str "#"))))))

(defn update-attr [node attr f]
  (.setAttribute node attr (f (.getAttribute node attr))))

(defn finalize-page [req page]
  (if (and (re-find #"\.html$" (:uri req)) (string? page))
    (html-walker/replace
     page
     {[:pre] maybe-highlight
      [:img] #(update-attr % "src" (optimize-path-fn req))
      [:h2] add-anchor
      [:h3] add-anchor
      [:h4] add-anchor})
    page))

(comment
  (highlight-code-blocks
   "<pre><code class=\"clj\">(defproject cjohansen-no \"0.1.0-SNAPSHOT\"
  :description \"cjohansen.no source code\"
  :url \"http://cjohansen.no\"
  :license {:name \"BSD 2 Clause\"
            :url \"http://opensource.org/licenses/BSD-2-Clause\"}
  :dependencies [[org.clojure/clojure \"1.5.1\"]
                 [stasis \"1.0.0\"]
                 [ring \"1.2.1\"]]
  :ring {:handler cjohansen-no.web/app}
  :profiles {:dev {:plugins [[lein-ring \"0.8.10\"]]}})</code></pre>")

  (highlight-code-blocks
   (slurp (clojure.java.io/file "/tmp/stuff.html")))

  (-> "(defproject cjohansen-no \"0.1.0-SNAPSHOT\"
  :description \"cjohansen.no source code\"
  :url \"http://cjohansen.no\"
  :license {:name \"BSD 2 Clause\"
            :url \"http://opensource.org/licenses/BSD-2-Clause\"}
  :dependencies [[org.clojure/clojure \"1.5.1\"]
                 [stasis \"1.0.0\"]])"
      (pygments/highlight :clj :html)
      prn)

  (-> "lein new cjohansen-no
cd cjohansen-no"
      (pygments/highlight :sh :html)
      prn)



  (-> (slurp (clojure.java.io/file "/Users/christian/projects/hafslund/consumptor/recipients.js"))
      (pygments/highlight :js :html)
      prn)
  )
