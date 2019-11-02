(ns cjohansen-no.dev
  (:require [cjohansen-no.web :as web]
            [clojure.tools.namespace.repl :as repl]
            [mount.core :as mount :refer [defstate]]
            [prone.middleware :as prone]
            [ring.adapter.jetty :as jetty]))

(repl/set-refresh-dirs "src" "dev")

(defstate server
  :start (jetty/run-jetty
          (-> (web/app-handler)
              prone/wrap-exceptions)
          {:port 3030
           :async? true
           :join? false})
  :stop (.stop server))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (start))

(defn reload []
  (stop)
  (repl/refresh :after 'cjohansen-no.dev/start))

(defn -main [& args]
  (start))

(comment
  (stop)
  (start)
  (restart)
)
