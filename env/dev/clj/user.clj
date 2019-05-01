(ns user
  (:require
    [multi-client-ws.config :refer [env]]
    [clojure.spec.alpha :as s]
    [expound.alpha :as expound]
    [mount.core :as mount]
    [multi-client-ws.figwheel :refer [start-fw stop-fw cljs]]
    [multi-client-ws.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'multi-client-ws.core/repl-server))

(defn stop []
  (mount/stop-except #'multi-client-ws.core/repl-server))

(defn restart []
  (stop)
  (start))


