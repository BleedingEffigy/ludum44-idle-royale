(ns multi-client-ws.routes.home
  (:require
    [multi-client-ws.layout :as layout]
    [clojure.java.io :as io]
    [multi-client-ws.middleware :as middleware]
    [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn test-page []
  [:div.col-md-12
   [:h2 "Welcome to chat"]])

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

