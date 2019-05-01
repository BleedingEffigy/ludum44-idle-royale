(ns multi-client-ws.components.pattern-gen
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            ))

(def width 220)
(def height 240)
(def mod_size 4)
(def color_min 90)
(def color_max 240)


(defn setup []
  (let [color_bg (q/color (q/random color_min color_max) (q/random color_min color_max) (q/random color_min color_max))
        fg_colors (q/floor (q/random 1 3) )
        color_fg (if (= fg_colors 2) [(q/color (q/random color_min color_max) (q/random color_min color_max) (q/random color_min color_max))
                                      (q/color (q/random color_min color_max) (q/random color_min color_max) (q/random color_min color_max))]
                     [ (q/color (q/random color_min color_max) (q/random color_min color_max) (q/random color_min color_max)) ])
        matrix_width (q/floor (q/random 3 9) )
        matrix_height (q/floor (q/random 3 7) )
        mirror_horizontal (q/floor (q/random 0 2) )
        mirror_vertical (q/floor (q/random 0 2) )
        gr (q/create-graphics width height)]
                                        ; Set frame rate to 30 frames per second.
    (q/frame-rate 20)
                                        ; Set color mode to HSB (HSV) instead of default RIB.
    (q/color-mode :rgb)
    (def pattern-atom (atom (vec (take matrix_width (repeat (vec (take matrix_height (repeat 0))))))))
    (println matrix_width matrix_height (zero? mirror_horizontal ) mirror_vertical fg_colors color_fg )
    (doseq [x (range matrix_width)
            y (range matrix_height)]
                                        ;check if pattern should be mirrored
      (if (or (and (not (zero? mirror_horizontal)) (> y (/ matrix_height 2))) (and (not (zero? mirror_vertical)) (> x (/ matrix_width 2))))
                                        ;check for which type of mirroring, if needed
        (if (and (not (zero? mirror_horizontal)) (> y (/ matrix_height 2))) (swap! pattern-atom assoc-in [x y] (get-in @pattern-atom [x (- matrix_height y)])) (swap! pattern-atom assoc-in [x y] (get-in @pattern-atom [(- matrix_width x) y])))
                                        ;provides color to texture, randomly chooses
        (let [rand_col (q/floor (q/random 0 (+ fg_colors 1)) )]
          (if (> rand_col 0) (swap! pattern-atom assoc-in [x y] rand_col)))
        ))
    (q/with-graphics gr
      (q/background color_bg)
      (q/rect width height 80 80)
      (doseq [x (range width)
              y (range height)
              :let [pattern-value (get-in @pattern-atom [(mod (/ x mod_size) matrix_width) (mod (/ y mod_size) matrix_height)])]]
        (when (> pattern-value 0) (q/set-pixel gr x y (color_fg (- pattern-value 1))) ))
      (q/update-pixels gr))
  (println @pattern-atom)
  {:color 0
   :graphic gr
   }))

(defn update-state [state]
                                        ; Update sketch state by changing circle color and position.
                                        ;(def vel [q/mouse-x q/mouse-y])
                                        ;(def vel [(some? (q/key-as-key)) ])
                                        ;(println state )
    (assoc state
           :color (mod (+ (:color state) 0.7) 255)
            ))

(defn draw-state [state]

                                        ; Clear the sketch by filling it with light-grey color.
    (q/background 240)
    (q/fill 0)
                                        ; Set circle color.
    (q/fill (:color state) 255 255)
                                        ; Calculate x and y coordinates of the circle.
    

  (q/image (:graphic state) 150 150)
  (q/ellipse 100 100 64 64)
  )

(defn ^:export run-sketch []
  (q/defsketch drdc
    :host "main"
    :size [600 600]
                                        ; setup function called only once, during sketch initialization.
    :setup setup
                                        ; update-state is called on each iteration before draw-state.
    :update update-state
    :draw draw-state
                                        ; This sketch uses functional-mode middleware.
                                        ; Check quil wiki for more info about middlewares and particularly
                                        ; fun-mode.
    :middleware [m/fun-mode]))
