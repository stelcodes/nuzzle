(ns codes.stel.nuzzle.ring
  (:require [ring.middleware.resource :refer [wrap-resource]]))

(defn wrap-static-dir
  [app static-dir]
  (if static-dir (wrap-resource app static-dir) app))
