(ns nuzzle.ring
  (:require
   [nuzzle.util :as util]
   [ring.middleware.file :refer [wrap-file]]))

(defn wrap-static-dir
  [app static-dir]
  (if static-dir
    (do
      (util/ensure-static-dir static-dir)
      (wrap-file app static-dir))
    app))
