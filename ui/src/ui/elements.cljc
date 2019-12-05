(ns ui.elements
  (:require [clojure.string :as str]))

(defn el [el params text]
  [(get params :element el)
   {:className (str (name el)
                    (when-let [cn (:className params)]
                      (str " " cn)))} text])

(def h1 (partial el :h1))
(def h2 (partial el :h2))
(def h3 (partial el :h3))
(def h4 (partial el :h4))
(def h5 (partial el :h5))

(def headings [nil :h1.h1 :h2.h2 :h3.h3 :h4.h4 :h5.h5])

(defn byline [{:keys [published updated tags]}]
  [:p.byline.text-s
   (cond
     (and published updated)
     [:span.date (str updated " (published " published ")")]

     published [:span.date published])
   (when tags
     [:span.subtle (->> (for [{:keys [title url]} tags]
                          [:a {:href url} title])
                        (interpose ", "))])])

(defn theme-class [theme]
  (if (keyword? theme)
    (str " theme-" (name theme))
    " theme-default"))

(defn type-class [type]
  (when (keyword? type)
    (name type)))

(defn section-class [{:keys [class theme type]} & [class-name]]
  (str/join " " (remove empty? [class-name class (theme-class theme) (type-class type)])))

(defn section [{:keys [title sub-title content media meta heading-level note] :as props}]
  (let [heading-level (or heading-level 1)]
    [:div.section {:className (section-class props)}
     [:div.content
      (when media
        [:div.media
         media])
      [:div.section-content
       [:div.text-content
        (when title [(nth headings heading-level) title])
        (when note [:p.note.text-s note])
        (when (:byline props) (byline (:byline props)))
        (when sub-title [(nth headings (inc heading-level)) sub-title])
        (when content content)
        (when (or (:published meta) (:updated meta))
          [:p.subtle.text-s
           (if (and (:published meta) (:updated meta))
             (str "Published " (:published meta) ", updated " (:updated meta))
             (str "Published " (:published meta)))])]]]]))

(defn split [{:keys [front back]}]
  [:div.split
   [:div.front front]
   [:div.back back]])

(defn centered [params]
  (section (assoc params :class "centered")))

(defn section-media-front [params]
  (section (assoc params :class "media-front")))

(defn section-media-back [params]
  (section (assoc params :class "media-back")))

(defn section-media [{:keys [media title size] :as props}]
  [:div.media-wide
   {:className
    (section-class
     props
     (if size
       (str "s-" (name size))
       "section"))}
   [:div.content
    [:div.media media]
    (when title [:div.title (h1 {} title)])]])

(defn header []
  [:div.header
   [:div.header-content
    [:a.logo {:href "/"}
     [:div.facebox
      [:img.img {:src "/images/christian-bw.png" :width 100}]
      [:img.img.hover {:src "/images/christian.png" :width 100}]]
     [:div.logo-name
      [:div.logo-firstname "Christian"]
      [:div.logo-lastname "Johansen"]]]
    [:ul.nav-list.menu {:role "nav"}
     [:li [:a {:href "/"} "Tech"]]
     [:li [:a {:href "/fermentations/"} "Fermentations"]]
     [:li [:a {:href "/about/"} "About" [:span.hide-mobile " me"]]]]]])

(defn simple-header []
  [:div.simple-header
   [:div.header-content
    [:a {:href "/"} "Christian Johansen"]]])

(defn now-year []
  #?(:clj (.getYear (java.time.LocalDate/ofInstant (java.time.Instant/now) (java.time.ZoneId/of "Europe/Oslo")))
     :cljs (.getFullYear (js/Date.))))

(defn footer []
  [:div.footer
   [:div.footer-content
    [:p.license
     [:a {:href "https://creativecommons.org/licenses/by-nc-sa/3.0/"
          :rel "license"
          :title "Creative Commons License"}
      [:span.cc-logo [:img {:src "/images/cc_icon_white_x2.png"}]]
      [:span.cc-logo [:img {:src "/images/attribution_icon_white_x2.png"}]]
      [:span.cc-logo [:img {:src "/images/nc_white_x2.png"}]]
      [:span.cc-logo [:img {:src "/images/sa_white_x2.png"}]]]
     (str "2006 - " (now-year))]
    [:p.twitter
     [:a {:href "https://twitter.com/cjno"}
      "Follow me (@cjno) on Twitter"]]
    [:p.email
     [:a {:href "mailto:christian@cjohansen.no"}
      "christian@cjohansen.no"]]]])

(defn blockquote [quote & [source]]
  [:blockquote.bq.text-content
   (when source
     [:div.bq-source
      [:p source]])
   [:div.bq-quote
    [:p quote]]])

(defn kv-table [items]
  [:table.table.text-xs
   (for [{:keys [label val]} items]
     [:tr
      [:th label]
      [:td [:strong val]]])])

(defn ingredient-list [ingredients]
  [:table.table.text-xs
   (for [{:keys [amount percent temp title]} ingredients]
     [:tr
      [:th title (when temp [:span.subtle (str " (" temp ")")])]
      [:td amount]
      [:td [:strong percent]]])])

(defn captioned [{:keys [content caption class]}]
  [:div.captioned {:className class}
   content
   [:div.caption [:p caption]]])

(def caption-themes
  {:red ""
   :blue "captioned-b"
   :green "captioned-g"
   :light "captioned-light"})

(defn captioned-image [{:keys [src caption alt theme pop?]}]
  (captioned
   {:content [:img.img {:src src
                        :alt (or alt caption)}]
    :caption caption
    :class (str (caption-themes theme) (when pop? " captioned-pop"))}))

(defn teaser [{:keys [url media title description pitch] :as props}]
  [:div.teaser
   (when media [:div.media [:a {:href url} media]])
   [:div.teaser-content
    (when pitch
      (h5 {:className "subtle"} pitch))
    (h4 {} [:a {:href url} title])
    description
    (byline props)]])

(defn teaser-section [{:keys [title sub-title teasers] :as props}]
  [:div.section {:className (section-class props "teasers")}
   [:div.content
    [:div.section-content
     (when title (h2 {} title))
     (when sub-title (h2 {} sub-title))
     [:div.teaser-list (map teaser teasers)]]]])
