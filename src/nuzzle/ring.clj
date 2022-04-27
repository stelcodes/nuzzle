(ns nuzzle.ring
  (:require
   [nuzzle.config :as conf]
   [nuzzle.log :as log]
   [nuzzle.generator :as gen]
   [nuzzle.util :as util]
   [ring.middleware.file :refer [wrap-file]]
   [stasis.core :as stasis]))

(defn wrap-overlay-dir
  [app]
  (fn [{:keys [config] :as request}]
    (let [{:keys [overlay-dir]} config]
      (if overlay-dir
        (do
          (util/ensure-overlay-dir overlay-dir)
          (log/log-overlay-dir overlay-dir)
          (-> request
              (assoc :config config)
              ((wrap-file app overlay-dir))))
        (app request)))))

(defn wrap-serve-pages
  "A wrapper around stasis.core/serve-pages which allows the get-pages function
  to access the request map. This allows the config to be passed down from
  wrap-overlay-dir, avoiding an unecessary config load"
  []
  (fn [{:keys [config] :as request}]
    (let [get-pages #(gen/generate-site-index config true)
          app (stasis/serve-pages get-pages)]
      (app request))))

(defn wrap-load-config
  "Loads config and adds it to the request map under they key :config"
  [app config-overrides]
  (fn [request]
    (let [config (conf/load-default-config config-overrides)]
      (-> request
          (assoc :config config)
          (app)))))
