(ns nuzzle.api
  (:require [nuzzle.config :as conf]
            [nuzzle.generator :as gen]
            [nuzzle.publish :as publish]
            [nuzzle.log :as log]
            [nuzzle.ring :as ring]))

(defn realize
  "Allows the user to visualize the site data after Nuzzle's modifications."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (let [config (conf/load-default-config config-overrides)]
    (log/info "🔍🐈 Printing realized config for inspection")
    (-> config gen/realize-config)))

(defn publish
  "Publishes the website to :nuzzle/publish-dir. The overlay directory is
  overlayed on top of the publish directory after the web pages have been
  published."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (-> config-overrides
      (conf/load-default-config)
      (publish/publish-site)))

(defn serve
  "Starts a server using http-kit for development."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (ring/start-server config-overrides))
