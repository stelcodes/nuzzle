(ns codes.stel.nuzzle.core
  (:require [codes.stel.nuzzle.generator :as gen]
            [ring.middleware.resource :refer [wrap-resource]]
            [stasis.core :as stasis]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :as log]))

(defn inspect
  "Allows the user to inspect the site-config after modifications such as the
  drafts being optionally removed, the group and tag index pages being added,
  and :uri and :render-resource fields being added."
  [{:keys [site-config remove-drafts?]}]
  (-> site-config
      (gen/load-site-config)
      (gen/realize-site-config remove-drafts?)))
