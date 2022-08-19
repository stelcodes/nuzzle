(ns nuzzle.server
  (:require
   [nuzzle.config :as conf]
   [nuzzle.log :as log]
   [nuzzle.util :as util]
   [org.httpkit.server :as http]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.file :refer [wrap-file]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace]]
   [stasis.core :as stasis]))

(defn wrap-overlay-dir
  [app]
  (let [last-overlay-dir (atom nil)]
    (fn [{:keys [config] :as request}]
      (let [{:nuzzle/keys [overlay-dir]} config]
        (if overlay-dir
          (do
            (when-not (= overlay-dir @last-overlay-dir)
              (log/log-overlay-dir overlay-dir)
              (reset! last-overlay-dir overlay-dir))
            (util/ensure-overlay-dir overlay-dir)
            (-> request
                ((wrap-file app overlay-dir))))
          (app request))))))

(defn handle-page-request
  "Handler that wraps around stasis.core/serve-pages, allowing the get-pages
  function (Stasis terminology) to access the request map. This allows the
  config to be passed down from wrap-overlay-dir, avoiding an unecessary config
  load"
  [{:keys [config] :as request}]
  (let [get-pages #(conf/create-site-index config :lazy-render? true)
        app (stasis/serve-pages get-pages)]
    (app request)))

(defn wrap-load-config
  "Loads config and adds it to the request map under they key :config"
  [app config-overrides]
  (fn [request]
    (let [config (conf/load-default-config config-overrides)]
      (-> request
          (assoc :config config)
          (app)))))

(defn start-server [config-overrides]
  (let [{:nuzzle/keys [server-port]} (conf/load-default-config config-overrides)]
    (log/log-start-server server-port)
    (-> handle-page-request
        (wrap-overlay-dir)
        (wrap-load-config config-overrides)
        (wrap-content-type)
        (wrap-stacktrace)
        (http/run-server {:port server-port}))))
