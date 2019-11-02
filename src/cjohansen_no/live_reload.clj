(ns cjohansen-no.live-reload
  (:require [clojure.core.async :refer [<! chan close! go put!]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hawk.core :as hawk]
            [ring.core.protocols :refer [StreamableResponseBody]])
  (:import java.io.EOFException))

(defn stream-response [ch watcher]
  {:status 200
   :headers {"Content-Type" "text/event-stream"}
   :body (reify StreamableResponseBody
           (write-body-to-stream [_ _ output]
             (letfn [(cleanup []
                       (println "Close channel and stop watcher")
                       (close! ch)
                       (hawk/stop! watcher))]
               (go (with-open [writer (io/writer output)]
                     (loop []
                       (when-let [msg (<! ch)]
                         (try
                           (doto writer
                             (.write msg)
                             (.flush))
                           (catch java.io.EOFException e
                             (println "Client likely closed the connection")
                             (cleanup))
                           (catch Throwable e
                             (println "Unexpected exception")
                             (prn e)
                             (cleanup)))
                         (recur))))))))})

(defn stream-msg [payload]
  (str "data:" (pr-str payload) "\n\n"))

(defn live-reload-handler [{:keys [before-reload]} req respond raise]
  (let [ch (chan)
        watcher (hawk/watch! [{:paths ["src" "resources"]
                               :handler (fn [ctx e]
                                          (when (ifn? before-reload)
                                            (before-reload e))
                                          (put! ch (stream-msg {:kind (:kind e)
                                                                :file (.getPath (:file e))}))
                                          ctx)}])]
    (respond (stream-response ch watcher))))

(defn script []
  "\n<script type=\"text/javascript\">new EventSource(\"/live-reload\").onmessage = function () { location.reload(true); };</script>")

(defn inject-script [body]
  (if (re-find #"</body>" body)
    (str/replace body "</body>" (str (script) "</body>"))
    (str body (script))))

(defn header-key [res n]
  (let [header-name (str/lower-case n)]
    (->> (keys (:headers res))
         (filter #(= header-name (str/lower-case %)))
         first)))

(defn header [res header-name & [default-val]]
  (get-in res [:headers (header-key res header-name)] default-val))

(defn wrap-live-reload [handler & [opt]]
  (fn [req respond raise]
    (if (= "/live-reload" (:uri req))
      (live-reload-handler opt req respond raise)
      (handler req (fn [response]
                     (respond
                      (if (and (re-find #"html" (header response "content-type" ""))
                               (string? (:body response)))
                        (update response :body inject-script)
                        response))) raise))))
