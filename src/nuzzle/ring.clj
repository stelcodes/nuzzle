(ns nuzzle.ring
  (:require
   [nuzzle.util :as util]
   [ring.middleware.file :refer [wrap-file]]))

(defn wrap-overlay-dir
  [app overlay-dir]
  (if overlay-dir
    (do
      (util/ensure-overlay-dir overlay-dir)
      (wrap-file app overlay-dir))
    app))
