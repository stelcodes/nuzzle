(ns nuzzle.api
  (:require [lambdaisland.deep-diff2 :as ddiff]
            [nuzzle.config :as conf]
            [nuzzle.publish :as publish]
            [nuzzle.log :as log]
            [nuzzle.server :as server]))

(defn transform
  "Allows the user to visualize the site data after Nuzzle's modifications."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (log/info "🔍🐈 Returning transformed config")
  (conf/load-default-config :config-overrides config-overrides))

(defn transform-diff
  "Pretty prints the diff between the config in nuzzle.edn and the config after
  Nuzzle's transformations."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (let [raw-config (conf/read-config-from-path "nuzzle.edn")
        transformed-config (conf/load-default-config :config-overrides config-overrides)]
    (log/info "🔍🐈 Printing Nuzzle's config transformations diff")
    (ddiff/pretty-print (ddiff/diff raw-config transformed-config))))

(defn publish
  "Publishes the website to :nuzzle/publish-dir. The overlay directory is
  overlayed on top of the publish directory after the web pages have been
  published."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (-> (conf/load-default-config :config-overrides config-overrides)
      (publish/publish-site)))

(defn serve
  "Starts a server using http-kit for development."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (server/start-server :config-overrides config-overrides))
