(ns cjohansen-no.dev
  (:require [cjohansen-no.web :as web]
            [clojure.tools.namespace.repl :as repl]
            [mount.core :as mount :refer [defstate]]
            [prone.middleware :as prone]
            [ring.adapter.jetty :as jetty]))

(defstate server
  :start (jetty/run-jetty
          (-> #'web/app
              prone/wrap-exceptions)
          {:port 3030
           :join? false})
  :stop (.stop server))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (repl/refresh :after 'cjohansen-no.dev/start))

(defn -main [& args]
  (start))

(comment
  (stop)
  (start)
  (restart)
)