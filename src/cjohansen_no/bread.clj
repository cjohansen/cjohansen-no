(ns cjohansen-no.bread)

(defn combine-amounts [ingredients]
  (loop [result {:ingredient (-> ingredients first :step-ingredient/ingredient)
                 :amount 0}
         [{:step-ingredient/keys [amount]} & ingredients] ingredients]
    (if amount
      (recur (update result :amount + amount) ingredients)
      result)))

(defn bread-ingredients [recipe]
  (->> recipe
       :bread/sections
       (mapcat :section/ingredients)
       (remove :step-ingredient/indirect-use?)
       (group-by :step-ingredient/ingredient)
       vals
       (map combine-amounts)))

(defn weight [ingredients]
  (->> (map :amount ingredients)
       (reduce + 0)))

(defn liquid-weight [ingredients]
  (->> ingredients
       (filter (comp :ingredient/liquid-weight :ingredient))
       (map #(* (-> % :ingredient :ingredient/liquid-weight) (:amount %)))
       (reduce + 0)))

(defn flour-weight [ingredients]
  (->> ingredients
       (filter (comp #{:flour} :ingredient/type :ingredient))
       (map #(* (-> % :ingredient :ingredient/flour-weight) (:amount %)))
       (reduce + 0)))

(defn hydration [ingredients]
  (/ (liquid-weight ingredients)
     (flour-weight ingredients)))

(defn flour? [step-ingredient]
  (= :flour (-> step-ingredient :ingredient :ingredient/type)))

(defn liquid? [step-ingredient]
  (= :liquid (-> step-ingredient :ingredient :ingredient/type)))

(defn step-ingredients [ingredients]
  (map (fn [{:step-ingredient/keys [ingredient amount temperature]}]
         {:ingredient ingredient
          :amount amount
          :temperature temperature}) ingredients))

(defn prepare-ingredients
  ([ingredients] (prepare-ingredients ingredients ingredients))
  ([ingredients full-ingredients]
   (let [flour-weight (flour-weight full-ingredients)
         sorted (sort-by (comp - :amount) ingredients)]
     (->> (concat
           (filter flour? sorted)
           (filter liquid? sorted)
           (remove #(or (flour? %) (liquid? %)) sorted))
          (map (fn [{:keys [ingredient amount temperature]}]
                 {:ingredient (:ingredient/name ingredient)
                  :url (:browsable/url ingredient)
                  :amount amount
                  :temperature temperature
                  :bakers-ratio (/ amount flour-weight)}))))))

(defn total-time [bread-post]
  (->> bread-post
       :bread/sections
       (keep :section/time)
       (reduce + 0)))

(defn human-duration [time]
  (format "%02d:%02d" (int (/ time 60)) (mod time 60)))

(defn recipe [bread-post]
  (let [ingredients (bread-ingredients bread-post)]
    {:hydration (hydration ingredients)
     :time (-> bread-post total-time human-duration)
     :ingredients (prepare-ingredients ingredients)}))
