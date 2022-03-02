(ns codes.stel.nuzzle.ring
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [taoensso.timbre :as log]))

(defn wrap-static-dir
  [app static-dir]
  (if static-dir
    (do (log/info (str "Wrapping static resources directory: " static-dir))
      (wrap-resource app static-dir))
    (do (log/info "No static resource directory provided") app)))
