(ns multi-client-ws.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.core :as r :refer [atom]]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [multi-client-ws.ajax :as ajax]
    [multi-client-ws.events]
    [multi-client-ws.components.sketch :as sketch]
    [multi-client-ws.components.pattern-gen :as pg]
    [reitit.core :as reitit]
    [clojure.string :as string]
    [multi-client-ws.websockets :as ws])
  (:import goog.History))

(defonce messages (atom []))

(defn message-list []
  [:ul
   (for [[i message] (map-indexed vector @messages)]
     ^{:key i}
     [:li message])])

(defn message-input []
  (let [value (atom nil)]
  (fn []
    [:input.form-control
     {:type :text
      :placeholder "type in a message and press enter"
      :value @value
      :on-change #(reset! value (-> % .-target .-value))
      :on-key-down
      #(when (= (.-keyCode %) 13)
         (ws/send-transit-msg!
          {:message @value})
         (reset! value nil))}])))

(defn ws-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to chat"]]]
   [:div.row
    [:div.col-sm-6
     [message-list]]]
   [:div.row
    [:div.col-sm-6
     [message-input]]]])

(defn update-messages! [{:keys [message]}]
  (swap! messages #(vec (take 10 (conj % message)))))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :active (when (= page @(rf/subscribe [:page])) "active")}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "multi-client-ws"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span][:span][:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-end
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]
       [nav-link "#/wss" "WS" :ws] 
       [nav-link "#/bio" "Bio" :bio]
       [nav-link "#/sketch" "Sketch" :sketch]
       [nav-link "#/pattern" "Pattern Gen"]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn bio-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn sketch-page []
  [:div [:div#main "test"] [:script "js/app.js"] [:script (sketch/run-sketch)]])

(defn sketch-reagent-page []
  (r/create-class
  {:display-name "sketch component"

   :component-did-mount
   (fn [this]
     (sketch/run-sketch))

   :reagent-render
   (fn []
     [:div#main "the sketch component"])}))

(defn pattern-page []
  (r/create-class
   {:display-name "pattern generating component"

    :component-did-mount
    (fn [this]
      (pg/run-sketch))

    :reagent-render
    (fn []
      [:div#main "the pattern generator"])}))

(def pages
  {:home #'home-page
   :about #'about-page
   :ws #'ws-page
   :bio #'bio-page
   :sketch #'sketch-reagent-page
   :pg #'pattern-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :home]
     ["/about" :about]
     ["/wss" :ws]
     ["/bio" :bio]
     ["/sketch" :sketch]
     ["/pattern" :pg]]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (let [uri (or (not-empty (string/replace (.-token event) #"^.*#" "")) "/")]
          (rf/dispatch
            [:navigate (reitit/match-by-path router uri)]))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)])
  
  (ajax/load-interceptors!)
  (rf/dispatch [:fetch-docs])
  (hook-browser-navigation!)
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
  (mount-components))
