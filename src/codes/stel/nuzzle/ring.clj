(ns codes.stel.nuzzle.ring
  (:require [ring.middleware.file :refer [wrap-file]]))

(defn wrap-static-dir
  [app static-dir]
  (if static-dir
    (try
      (wrap-file app static-dir)
      (catch Exception _
        (throw (ex-info (str "Static directory " static-dir " does not exist")
                        {:static-dir static-dir}))))
    app))
