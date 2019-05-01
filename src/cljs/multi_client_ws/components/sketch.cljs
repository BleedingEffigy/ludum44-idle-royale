(ns multi-client-ws.components.sketch
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)])
    (:require [quil.core :as q :include-macros true]
              [quil.middleware :as m]
              [cljs.core.async :as async :refer (<! >! put! chan)]
              [multi-client-ws.websockets :as ws]))

(def width 700)
(def height 700)
(def game-width 3000)
(def game-height 3000)

; Values and functions for io game

(def velocity-state (atom {:x 0 :y 0}))

(defn new-blob [x y r]
    {:position {:x x :y y}
     :radius r
     } )

(defn show-blob [blob]
  (let [{{:keys [x y]} :position r :radius} blob]
    (q/fill 255)
    (q/ellipse x y (* r 2) (* r 2)) ))

(defn eat? [x y r mini-blob]
  (let [
       total (count mini-blob)
       not-eaten (remove
               #(< (q/dist x y (get-in % [:position :x]) (get-in % [:position :y])) (+ r (:radius %)))
               mini-blob)
        ]
    [(- total (count not-eaten)) not-eaten]
    ))

(defn setup []
  ; Set frame rate to 30 frames per second.
  (q/frame-rate 20)
  ; Set color mode to HSB (HSV) instead of default RGB.
  (q/color-mode :hsb)
  ; setup function returns initial state. It contains
  ; circle color and position.
  (ws/send-transit-msg! {:x (/ width 2)
                         :y (/ height 2) })
  {:color 0
   :main-blob (new-blob (/ width 2) (/ height 2) 64)
   :velocity {:x 0 :y 0}
   :mini-blobs (for [i (range 700)] (conj {} (new-blob (- (rand-int (* 2 game-width )) game-width) (- (rand-int (* 2 game-height )) game-height) 16)))
   :zoom 1})

(defn update-state [state]
  ; Update sketch state by changing circle color and position.
  ;(def vel [q/mouse-x q/mouse-y])
  ;(def vel [(some? (q/key-as-key)) ])
  ;(println state )
  (let [
        {{ {:keys [x y]} :position r :radius} :main-blob {vx :x vy :y} :velocity} state
        [num-eaten mini-blobs] (eat? x y r (:mini-blobs state))
        ]
    (assoc state
           ;:color (mod (+ (:color state) 0.7) 255)
           :main-blob (new-blob (if (zero? vx) x (+ x vx)) (if (zero? vy) y (+ y vy)) (q/sqrt (+ (* r r) (* 64 num-eaten))))
           ;check if blob eats/collides with other
           :mini-blobs mini-blobs
           ;update zoom level
           :zoom (q/lerp (:zoom state) (/ 64 r) 0.1)
    ) ))

(defn draw-state [state]
  (let [{{{:keys [x y]} :position r :radius} :main-blob} state]

  ; Clear the sketch by filling it with light-grey color.
  (q/background 240)
  ;display blob coordinates
  (q/fill 0)
  (q/text (str x ", " y) 10 10)

  ; Translate camera to keep blob centered
  (q/translate (/ width 2) (/ height 2))
  ;scale camera to player size
  (q/scale (:zoom state))
  ;fix camera offsetting from growth
  (q/translate (- 0 x) (- 0 y))
  ; Set circle color.
  (q/fill (:color state) 255 255)
  ;init and show blob
  (show-blob (state :main-blob))
  (doseq [blob (:mini-blobs state)]
    (show-blob blob))
  ; Calculate x and y coordinates of the circle.
 ; (let [angle (:angle state)
 ;       x (* 150 (q/cos angle))
 ;       y (* 150 (q/sin angle))]
 ;   ; Move origin point to the center of the sketch.
 ;   (q/with-translation [(/ (q/width) 2)
 ;                        (/ (q/height) 2)]
 ;     ; Draw the circle.
 ;     (q/ellipse x y 100 100)))
 ))

(defn key-pressed [state event]
  ;(println "a key was pressed" (event :key))
  (let [key (event :key)
        vx (get-in state [:velocity :x])
        vy (get-in state [:velocity :y])]
    (case key
      :ArrowUp (assoc-in state [:velocity :y] (- vy 10))
      :ArrowDown (assoc-in state [:velocity :y] (+ vy 10))
      :ArrowLeft (assoc-in state [:velocity :x] (- vx 10))
      :ArrowRight (assoc-in state [:velocity :x] (+ vx 10))
      state))
  )

(defn key-released [state event]
  (let [key (event :key)
        vx (get-in state [:velocity :x])
        vy (get-in state [:velocity :y]) ]
    (case key
      :ArrowUp (assoc-in state [:velocity :y] (+ vy 10))
      :ArrowDown (assoc-in state [:velocity :y] (- vy 10))
      :ArrowLeft (assoc-in state [:velocity :x] (+ vx 10))
      :ArrowRight (assoc-in state [:velocity :x] (- vx 10))
      state)))

; this function is called in index.html
(defn ^:export run-sketch []
  (q/defsketch drdc
    :host "main"
    :size [width height]
    ; setup function called only once, during sketch initialization.
    :setup setup
    ; update-state is called on each iteration before draw-state.
    :update update-state
    :draw draw-state
    :key-pressed key-pressed
    :key-released key-released
    ; This sketch uses functional-mode middleware.
    ; Check quil wiki for more info about middlewares and particularly
    ; fun-mode.
    :middleware [m/fun-mode]))

