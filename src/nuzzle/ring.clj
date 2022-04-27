(ns nuzzle.ring
  (:require
   [nuzzle.config :as conf]
   [nuzzle.log :as log]
   [nuzzle.generator :as gen]
   [nuzzle.util :as util]
   [ring.middleware.file :refer [wrap-file]]
   [stasis.core :as stasis]))

(defn wrap-overlay-dir
  [app config-overrides]
  (fn [request]
    (let [{:keys [overlay-dir] :as config} (conf/load-config config-overrides)]
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
  [config-overrides]
  (fn [{:keys [config] :as request}]
    (let [{:keys [render-webpage] :as config}
          (or config (conf/load-config config-overrides))
          get-pages #(-> config
                         (gen/realize-site-data)
                         (gen/generate-page-list)
                         (gen/generate-site-index render-webpage true))]
      ((stasis/serve-pages get-pages) request))))


