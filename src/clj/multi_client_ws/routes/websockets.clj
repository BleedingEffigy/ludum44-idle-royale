(ns multi-client-ws.routes.websockets
  (:require
   [clojure.tools.logging :as log]
   [immutant.web.async :as async]
   [cognitect.transit :as t]
   [clojure.data.json :as json]))


(defonce channels (atom #{}))
(defonce messages (atom nil))
(defonce users (atom nil))

(defn connect! [channel]
  ;(log/info "channel open " channel "msg: " ( get (json/read-str (nth @messages 1) ) "~:message" ))
  (swap! channels conj channel)
  ;(log/info channel)
  ;(doseq [msg @messages]
  ;  (async/send! channel msg))
  )

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (swap! channels #(remove #{channel} %)))

;called when server receives msg from client. Broadcasts to all the clients.
(defn notify-clients! [channel msg]
  ;(def in (ByteArrayInputStream. msg))
  ;(log/info "reader: " in)
  (doseq [channel @channels]
    (async/send! channel msg))
  (swap! messages conj msg))

(def websocket-callbacks
  "Web socket callback functions"
  {:on-open connect!
   :on-close disconnect!
   :on-message notify-clients!})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

(def websocket-routes
  ["/ws" ws-handler])


