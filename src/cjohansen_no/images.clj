(ns cjohansen-no.images)

(def image-asset-config
  {:prefix "image-assets"
   :resource-path "public"
   :disk-cache? false
   :transformations

   {:short-wide
    {:transformations [[:fit {:width 1600 :height 600}]]
     :retina-optimized? true
     :width 1600
     :height 600}

    :thumb
    {:transformations [[:fit {:width 270 :height 135}]]
     :retina-optimized? true
     :width 270
     :height 135}

    :vert
    {:transformations [[:fit {:width 410 :height 550}]]
     :retina-optimized? true
     :width 410
     :height 550}

    :vert-wide
    {:transformations [[:fit {:width 410 :height 350}]]
     :retina-optimized? true
     :width 410
     :height 350}}})
