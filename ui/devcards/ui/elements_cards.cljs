(ns ui.elements-cards
  (:require [dumdom.devcards :refer-macros [defcard]]
            [dumdom.core :as dd]
            [dumdom.dom :as d]
            [ui.elements :as e]))

(defcard h1
  (e/h1 {} "Clojure in Production with tools.deps"))

(defcard h2-acting-as-h1
  (e/h1 {:element :h2} "Clojure in Production with tools.deps"))

(defcard h2
  (e/h2 {} "Tying it all together"))

(defcard h1-acting-as-h2
  (e/h2 {:element :h1} "Some headings are quite long and will wrap - when they do they should still be legible"))

(defcard h3
  (e/h3 {} "Tying it all together"))

(defcard h1-acting-as-h3
  (e/h3 {:element :h1} "Some headings are quite long and will wrap - when they do they should still be legible"))

(defcard h4
  (e/h4 {} "Tying it all together"))

(defcard h1-acting-as-h4
  (e/h4 {:element :h1} "Some headings are quite long and will wrap - when they do they should still be legible"))

(defcard paragraphs
  [:p {} "I'm hoping this post will not only show you the power of Clojure and Stasis for building static web sites, but also give you a good introduction to some very useful Clojure libraries. Maybe even to Clojure itself. In particular, I will discuss using these libraries: Stasis, Optimus, enlive, hiccup, cegdown, clygments and even write some tests with Midje."])

(defcard hilite-code-sample
  [:pre.codehilite
    [:code
     [:span]
     [:span.p "("]
     [:span.kd "defproject "]
     [:span.nv "cjohansen-no"]
     " "
     [:span.s "\"0.1.0-SNAPSHOT\""]
     "\n  "
     [:span.ss ":description"]
     " "
     [:span.s "\"cjohansen.no source code\""]
     "\n  "
     [:span.ss ":url"]
     " "
     [:span.s "\"http://cjohansen.no\""]
     "\n  "
     [:span.ss ":license"]
     " "
     [:span.p "{"]
     [:span.ss ":name"]
     " "
     [:span.s "\"BSD 2 Clause\""]
     "\n            "
     [:span.ss ":url"]
     " "
     [:span.s "\"http://opensource.org/licenses/BSD-2-Clause\""]
     [:span.p "}"]
     "\n  "
     [:span.ss ":dependencies"]
     " "
     [:span.p "[["]
     [:span.nv "org.clojure/clojure"]
     " "
     [:span.s "\"1.5.1\""]
     [:span.p "]"]
     "\n                 "
     [:span.p "["]
     [:span.nv "stasis"]
     " "
     [:span.s "\"1.0.0\""]
     [:span.p "]"]
     "\n                 "
     [:span.p "["]
     [:span.nv "ring"]
     " "
     [:span.s "\"1.2.1\""]
     [:span.p "]]"]
     "\n  "
     [:span.ss ":ring"]
     " "
     [:span.p "{"]
     [:span.ss ":handler"]
     " "
     [:span.nv "cjohansen-no.web/app"]
     [:span.p "}"]
     "\n  "
     [:span.ss ":profiles"]
     " "
     [:span.p "{"]
     [:span.ss ":dev"]
     " "
     [:span.p "{"]
     [:span.ss ":plugins"]
     " "
     [:span.p "[["]
     [:span.nv "lein-ring"]
     " "
     [:span.s "\"0.8.10\""]
     [:span.p "]]}})"]]])

(defcard html-code-sample
  [:pre.codehilite
   [:code
    [:span]
    [:span.cp "<!DOCTYPE html>"]
    "\n"
    [:span.p "<"] [:span.nt "html"] " " [:span.na "lang"] [:span.o "="] [:span.s "\"en\""] [:span.p ">"]
    "\n  "
    [:span.p "<"] [:span.nt "head"] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "meta"] " " [:span.na "charset"] [:span.o "="] [:span.s "\"utf-8\""] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "meta"] " " [:span.na "name"] [:span.o "="] [:span.s "\"viewport\""] " " [:span.na "content"] [:span.o "="] [:span.s "\"width=device-width, initial-scale=1\""] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "title"] [:span.p ">"]
    "Webslides Pygments Demo" [:span.p "</"] [:span.nt "title"] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "meta"] " " [:span.na "name"] [:span.o "="] [:span.s "\"description\""] " " [:span.na "content"] [:span.o "="] [:span.s "\"Syntax highlighting Webslides with Pygments\""] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "link"] " " [:span.na "href"] [:span.o "="] [:span.s "\"https://fonts.googleapis.com/css?family=Roboto:100,100i,300,300i,400,400i,700,700i%7CMaitree:200,300,400,600,700&amp;subset=latin-ext\""] " " [:span.na "rel"] [:span.o "="] [:span.s "\"stylesheet\""] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "link"] " " [:span.na "rel"] [:span.o "="] [:span.s "\"stylesheet\""] " " [:span.na "type"] [:span.o "="] [:span.s "'text/css'"] " " [:span.na "media"] [:span.o "="] [:span.s "'all'"] " " [:span.na "href"] [:span.o "="] [:span.s "\"static/css/webslides.css\""] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "link"] " " [:span.na "rel"] [:span.o "="] [:span.s "\"stylesheet\""] " " [:span.na "type"] [:span.o "="] [:span.s "'text/css'"] " " [:span.na "media"] [:span.o "="] [:span.s "'all'"] " " [:span.na "href"] [:span.o "="] [:span.s "\"static/css/prism.css\""] [:span.p ">"]
    "\n  "
    [:span.p "</"] [:span.nt "head"] [:span.p ">"]
    "\n  "
    [:span.p "<"] [:span.nt "body"] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "main"] " " [:span.na "role"] [:span.o "="] [:span.s "\"main\""] [:span.p ">"]
    "\n      "
    [:span.p "<"] [:span.nt "article"] " " [:span.na "id"] [:span.o "="] [:span.s "\"webslides\""] " " [:span.na "class"] [:span.o "="] [:span.s "\"vertical\""] [:span.p ">"]
    "\n        "
    [:span.p "<"] [:span.nt "section"] [:span.p ">"]
    "\n          "
    [:span.p "<"] [:span.nt "span"] " " [:span.na "class"] [:span.o "="] [:span.s "\"background\""] [:span.p "></"]
    [:span.nt "span"] [:span.p ">"]
    "\n          "
    [:span.p "<"] [:span.nt "div"] " " [:span.na "class"] [:span.o "="] [:span.s "\"wrap aligncenter\""] [:span.p ">"]
    "\n            "
    [:span.p "<"] [:span.nt "h1"] [:span.p "><"]
    [:span.nt "strong"] [:span.p ">"]
    "Webslides with Pygments" [:span.p "</"] [:span.nt "strong"] [:span.p "></"]
    [:span.nt "h1"] [:span.p ">"]
    "\n            "
    [:span.p "<"] [:span.nt "p"] [:span.p "><"]
    [:span.nt "a"] " " [:span.na "href"] [:span.o "="] [:span.s "\"/webslides-syntax-highlighting/\""] [:span.p ">"]
    "Read the post" [:span.p "</"] [:span.nt "a"] [:span.p "></"]
    [:span.nt "p"] [:span.p ">"]
    "\n          "
    [:span.p "</"] [:span.nt "div"] [:span.p ">"]
    "\n        "
    [:span.p "</"] [:span.nt "section"] [:span.p ">"]
    "\n        "
    [:span.p "<"] [:span.nt "section"] [:span.p ">"]
    "\n          "
    [:span.p "<"] [:span.nt "span"] " " [:span.na "class"] [:span.o "="] [:span.s "\"background\""] [:span.p "></"]
    [:span.nt "span"] [:span.p ">"]
    "\n          "
    [:span.p "<"] [:span.nt "div"] " " [:span.na "class"] [:span.o "="] [:span.s "\"wrap aligncenter\""] [:span.p ">"]
    "\n            "
    [:span.p "<"] [:span.nt "pre"] " " [:span.na "class"] [:span.o "="] [:span.s "\"language-clojure\""] [:span.p ">\n              <"]
    [:span.nt "code"] [:span.p ">"]
    "(println \"Hello world\")" [:span.p "</"] [:span.nt "code"] [:span.p ">\n            </"]
    [:span.nt "pre"] [:span.p ">"]
    "\n          "
    [:span.p "</"] [:span.nt "div"] [:span.p ">"]
    "\n        "
    [:span.p "</"] [:span.nt "section"] [:span.p ">"]
    "\n      "
    [:span.p "</"] [:span.nt "article"] [:span.p ">"]
    "\n    "
    [:span.p "</"] [:span.nt "main"] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "script"] " " [:span.na "src"] [:span.o "="] [:span.s "\"static/js/prism.js\""] [:span.p "></"]
    [:span.nt "script"] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "script"] " " [:span.na "src"] [:span.o "="] [:span.s "\"static/js/webslides.js\""] [:span.p "></"]
    [:span.nt "script"] [:span.p ">"]
    "\n    "
    [:span.p "<"] [:span.nt "script"] [:span.p ">"]
    [:span.nb "window"] [:span.p "."] [:span.nx "ws"] " " [:span.o "="] " " [:span.k "new"] " " [:span.nx "WebSlides"] [:span.p "();"] [:span.p "</"] [:span.nt "script"] [:span.p ">"]
    "\n  "
    [:span.p "</"] [:span.nt "body"] [:span.p ">"]
    "\n"
    [:span.p "</"] [:span.nt "html"] [:span.p ">"]]
   "\n"])

(dd/defcomponent CodeSample
  :on-render (fn [el code]
               (set! (.-innerHTML el) (str "<pre class=\"codehilite\"><code>"
                                           code
                                           "</code></pre>")))
  [data]
  [:div])

(defcard js-code-sample
  (CodeSample "<span></span><span class=\"kr\">const</span> <span class=\"nx\">fs</span> <span class=\"o\">=</span> <span class=\"nx\">require</span><span class=\"p\">(</span><span class=\"s1\">&#39;fs&#39;</span><span class=\"p\">);</span>\n<span class=\"kr\">const</span> <span class=\"nx\">recipients</span> <span class=\"o\">=</span> <span class=\"nx\">JSON</span><span class=\"p\">.</span><span class=\"nx\">parse</span><span class=\"p\">(</span><span class=\"nx\">fs</span><span class=\"p\">.</span><span class=\"nx\">readFileSync</span><span class=\"p\">(</span><span class=\"nx\">process</span><span class=\"p\">.</span><span class=\"nx\">argv</span><span class=\"p\">[</span><span class=\"mi\">2</span><span class=\"p\">],</span> <span class=\"s1\">&#39;utf-8&#39;</span><span class=\"p\">));</span>\n<span class=\"kr\">const</span> <span class=\"nx\">reservations</span> <span class=\"o\">=</span> <span class=\"nx\">require</span><span class=\"p\">(</span><span class=\"s1\">&#39;/tmp/all-reservations.json&#39;</span><span class=\"p\">);</span>\n<span class=\"kr\">const</span> <span class=\"nx\">campaign</span> <span class=\"o\">=</span> <span class=\"nx\">process</span><span class=\"p\">.</span><span class=\"nx\">argv</span><span class=\"p\">[</span><span class=\"mi\">3</span><span class=\"p\">];</span>\n<span class=\"kr\">const</span> <span class=\"p\">{</span><span class=\"nx\">KafkaClient</span><span class=\"p\">,</span> <span class=\"nx\">Producer</span><span class=\"p\">}</span> <span class=\"o\">=</span> <span class=\"nx\">require</span><span class=\"p\">(</span><span class=\"s1\">&#39;kafka-node&#39;</span><span class=\"p\">);</span>\n<span class=\"kr\">const</span> <span class=\"nx\">producer</span> <span class=\"o\">=</span> <span class=\"k\">new</span> <span class=\"nx\">Producer</span><span class=\"p\">(</span><span class=\"k\">new</span> <span class=\"nx\">KafkaClient</span><span class=\"p\">({}),</span> <span class=\"p\">{</span><span class=\"nx\">partitionerType</span><span class=\"o\">:</span> <span class=\"mi\">2</span><span class=\"p\">});</span>\n\n<span class=\"kd\">function</span> <span class=\"nx\">includesRecipient</span><span class=\"p\">(</span><span class=\"nx\">coll</span><span class=\"p\">,</span> <span class=\"p\">{</span><span class=\"nx\">hafslund_uid</span><span class=\"p\">,</span> <span class=\"nx\">cab_id</span><span class=\"p\">,</span> <span class=\"nx\">email</span><span class=\"p\">})</span> <span class=\"p\">{</span>\n  <span class=\"k\">return</span> <span class=\"nx\">coll</span>\n    <span class=\"p\">.</span><span class=\"nx\">filter</span><span class=\"p\">(</span><span class=\"nx\">r</span> <span class=\"o\">=&gt;</span> <span class=\"nx\">r</span><span class=\"p\">.</span><span class=\"nx\">hafslund_uid</span> <span class=\"o\">===</span> <span class=\"nx\">hafslund_uid</span> <span class=\"o\">||</span> <span class=\"nx\">r</span><span class=\"p\">.</span><span class=\"nx\">customer_id</span> <span class=\"o\">===</span> <span class=\"nx\">cab_id</span> <span class=\"o\">||</span> <span class=\"nx\">r</span><span class=\"p\">.</span><span class=\"nx\">email</span> <span class=\"o\">===</span> <span class=\"nx\">email</span><span class=\"p\">)</span>\n    <span class=\"p\">.</span><span class=\"nx\">length</span> <span class=\"o\">&gt;</span> <span class=\"mi\">0</span><span class=\"p\">;</span>\n<span class=\"p\">}</span>\n\n<span class=\"kd\">function</span> <span class=\"nx\">uniqRecipients</span><span class=\"p\">(</span><span class=\"nx\">recipients</span><span class=\"p\">)</span> <span class=\"p\">{</span>\n  <span class=\"k\">return</span> <span class=\"nx\">recipients</span><span class=\"p\">.</span><span class=\"nx\">reduce</span><span class=\"p\">((</span><span class=\"nx\">result</span><span class=\"p\">,</span> <span class=\"nx\">recipient</span><span class=\"p\">)</span> <span class=\"o\">=&gt;</span> <span class=\"p\">{</span>\n    <span class=\"k\">if</span> <span class=\"p\">(</span><span class=\"o\">!</span><span class=\"nx\">includesRecipient</span><span class=\"p\">(</span><span class=\"nx\">result</span><span class=\"p\">,</span> <span class=\"p\">{</span><span class=\"nx\">cab_id</span><span class=\"o\">:</span> <span class=\"nx\">recipient</span><span class=\"p\">.</span><span class=\"nx\">cab_id</span><span class=\"p\">}))</span> <span class=\"p\">{</span>\n      <span class=\"nx\">result</span><span class=\"p\">.</span><span class=\"nx\">push</span><span class=\"p\">(</span><span class=\"nx\">recipient</span><span class=\"p\">);</span>\n    <span class=\"p\">}</span>\n\n    <span class=\"k\">return</span> <span class=\"nx\">result</span><span class=\"p\">;</span>\n  <span class=\"p\">},</span> <span class=\"p\">[]);</span>\n<span class=\"p\">}</span>\n\n<span class=\"kd\">function</span> <span class=\"nx\">notReserved</span><span class=\"p\">(</span><span class=\"nx\">recipient</span><span class=\"p\">)</span> <span class=\"p\">{</span>\n  <span class=\"k\">return</span> <span class=\"o\">!</span><span class=\"nx\">includesRecipient</span><span class=\"p\">(</span><span class=\"nx\">reservations</span><span class=\"p\">,</span> <span class=\"nx\">recipient</span><span class=\"p\">);</span>\n<span class=\"p\">}</span>\n\n<span class=\"kd\">function</span> <span class=\"nx\">ednRecipient</span><span class=\"p\">({</span><span class=\"nx\">hafslund_uid</span><span class=\"p\">,</span> <span class=\"nx\">cab_id</span><span class=\"p\">,</span> <span class=\"nx\">email</span><span class=\"p\">})</span> <span class=\"p\">{</span>\n  <span class=\"k\">return</span> <span class=\"sb\">`{:hafslund-uid &quot;</span><span class=\"si\">${</span><span class=\"nx\">hafslund_uid</span><span class=\"si\">}</span><span class=\"sb\">&quot; :cab-id </span><span class=\"si\">${</span><span class=\"nx\">cab_id</span><span class=\"si\">}</span><span class=\"sb\"> :email &quot;</span><span class=\"si\">${</span><span class=\"nx\">email</span><span class=\"si\">}</span><span class=\"sb\">&quot; :utm-campaign &quot;</span><span class=\"si\">${</span><span class=\"nx\">campaign</span><span class=\"si\">}</span><span class=\"sb\">&quot;}`</span><span class=\"p\">;</span>\n<span class=\"p\">}</span>\n\n<span class=\"kd\">function</span> <span class=\"nx\">produce</span><span class=\"p\">(</span><span class=\"nx\">event</span><span class=\"p\">,</span> <span class=\"nx\">i</span><span class=\"p\">)</span> <span class=\"p\">{</span>\n  <span class=\"k\">return</span> <span class=\"k\">new</span> <span class=\"nb\">Promise</span><span class=\"p\">((</span><span class=\"nx\">resolve</span><span class=\"p\">,</span> <span class=\"nx\">reject</span><span class=\"p\">)</span> <span class=\"o\">=&gt;</span> <span class=\"p\">{</span>\n    <span class=\"nx\">producer</span><span class=\"p\">.</span><span class=\"nx\">send</span><span class=\"p\">([</span>\n      <span class=\"p\">{</span>\n        <span class=\"nx\">topic</span><span class=\"o\">:</span> <span class=\"s2\">&quot;consumption-report-customers&quot;</span><span class=\"p\">,</span>\n        <span class=\"nx\">partition</span><span class=\"o\">:</span> <span class=\"nx\">i</span> <span class=\"o\">%</span> <span class=\"mi\">8</span><span class=\"p\">,</span>\n        <span class=\"nx\">messages</span><span class=\"o\">:</span> <span class=\"p\">[</span><span class=\"nx\">event</span><span class=\"p\">]</span>\n      <span class=\"p\">}</span>\n    <span class=\"p\">],</span> <span class=\"p\">(</span><span class=\"nx\">err</span><span class=\"p\">,</span> <span class=\"nx\">data</span><span class=\"p\">)</span> <span class=\"o\">=&gt;</span> <span class=\"p\">{</span>\n      <span class=\"k\">if</span> <span class=\"p\">(</span><span class=\"nx\">err</span><span class=\"p\">)</span> <span class=\"p\">{</span>\n        <span class=\"nx\">reject</span><span class=\"p\">(</span><span class=\"nx\">err</span><span class=\"p\">);</span>\n      <span class=\"p\">}</span> <span class=\"k\">else</span> <span class=\"p\">{</span>\n        <span class=\"nx\">resolve</span><span class=\"p\">(</span><span class=\"nx\">data</span><span class=\"p\">);</span>\n      <span class=\"p\">}</span>\n    <span class=\"p\">});</span>\n  <span class=\"p\">});</span>\n<span class=\"p\">}</span>\n\n<span class=\"nb\">Promise</span><span class=\"p\">.</span><span class=\"nx\">all</span><span class=\"p\">(</span>\n  <span class=\"nx\">uniqRecipients</span><span class=\"p\">(</span><span class=\"nx\">recipients</span><span class=\"p\">.</span><span class=\"nx\">filter</span><span class=\"p\">(</span><span class=\"nx\">notReserved</span><span class=\"p\">))</span>\n    <span class=\"p\">.</span><span class=\"nx\">map</span><span class=\"p\">(</span><span class=\"nx\">ednRecipient</span><span class=\"p\">)</span>\n    <span class=\"p\">.</span><span class=\"nx\">map</span><span class=\"p\">(</span><span class=\"nx\">produce</span><span class=\"p\">)</span>\n<span class=\"p\">).</span><span class=\"nx\">then</span><span class=\"p\">(</span><span class=\"nx\">data</span> <span class=\"o\">=&gt;</span> <span class=\"p\">{</span>\n  <span class=\"nx\">console</span><span class=\"p\">.</span><span class=\"nx\">log</span><span class=\"p\">(</span><span class=\"nx\">JSON</span><span class=\"p\">.</span><span class=\"nx\">stringify</span><span class=\"p\">(</span><span class=\"nx\">data</span><span class=\"p\">,</span> <span class=\"kc\">undefined</span><span class=\"p\">,</span> <span class=\"mi\">2</span><span class=\"p\">));</span>\n  <span class=\"nx\">process</span><span class=\"p\">.</span><span class=\"nx\">exit</span><span class=\"p\">(</span><span class=\"mi\">0</span><span class=\"p\">);</span>\n<span class=\"p\">});</span>"))

(defcard header
  (e/header))

(defcard dark-header
  [:div.dark-theme1 (e/header)])
