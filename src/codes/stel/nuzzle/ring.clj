(ns codes.stel.nuzzle.ring
  (:require [ring.middleware.file :refer [wrap-file]]))

(defn wrap-static-dir
  [app static-dir]
  (if static-dir (wrap-file app static-dir) app))
