(ns nuzzle.api
  (:require [nuzzle.config :as conf]
            [nuzzle.export :as export]
            [nuzzle.log :as log]
            [nuzzle.ring :as ring]
            [nuzzle.util :as util]))

(defn realize
  "Allows the user to visualize the site data after Nuzzle's modifications."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (let [config (conf/load-default-config config-overrides)]
    (log/info "🔍🐈 Printing realized site data for inspection")
    (util/convert-site-data-to-vector config)))

(defn export
  "Exports the website to :export-dir. The :overlay-dir is overlayed on top of
  the :export-dir after the web pages have been exported."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (-> config-overrides
      (conf/load-default-config)
      (export/export-site)))

(defn serve
  "Starts a server using http-kit for development."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (ring/start-server config-overrides))
