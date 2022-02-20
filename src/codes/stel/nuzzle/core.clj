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

(defn export
  "Exports the website to :target-dir. The :static-dir is overlayed on top of
  the :target-dir after the web pages have been exported."
  [{:keys [static-dir target-dir] :as global-config}]
  {:pre [(or (map? global-config) (string? global-config))
         (string? static-dir)
         (string? target-dir)]}
  (log/info "🔨🐈 Exporting static site to disk")
  (-> global-config
      (gen/global-config->site-index)
      (gen/export-site-index static-dir target-dir)))
