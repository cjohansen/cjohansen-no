(ns ^:figwheel-hooks ui.devcards
  (:require [ui.color-cards]
            [ui.elements-cards]
            [ui.sections-cards]
            [devcards.core :as devcards]))

(enable-console-print!)

(defn render []
  (devcards/start-devcard-ui!))

(defn ^:after-load render-on-relaod []
  (render))

(render)
