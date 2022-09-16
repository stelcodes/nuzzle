(ns nuzzle.server
  (:require
   [nuzzle.pages :as pages]
   [nuzzle.log :as log]
   [nuzzle.util :as util]
   [org.httpkit.server :as http]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.file :refer [file-request]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace]]
   [stasis.core :as stasis]))

(defn wrap-overlay-dir
  [app overlay-dir]
  (if overlay-dir
    (fn [request]
      (if-let [response (file-request request overlay-dir)]
        response
        (app request)))
    (fn [request] (app request))))

(defn handle-page-request
  "Handler that wraps around stasis.core/serve-pages, if config is a var then
  the config is resolved and validated upon each request. Otherwise the config
  is validated once and the app is built just once."
  [pages]
  (if (var? pages)
    (fn [request]
      (let [loaded-pages (pages/load-pages pages)
            stasis-pages (pages/create-site-index loaded-pages :lazy-render? true)
            app (stasis/serve-pages stasis-pages)]
        (app request)))
    (let [loaded-pages (pages/load-pages pages)
          stasis-pages (pages/create-site-index loaded-pages :lazy-render? true)
          app (stasis/serve-pages stasis-pages)]
      (fn [request]
        (app request)))))

(defn start-server [pages & {:keys [port overlay-dir]}]
  (log/log-start-server port)
  (when overlay-dir
    (log/log-overlay-dir overlay-dir)
    (util/ensure-overlay-dir overlay-dir))
  (-> (handle-page-request pages)
      (wrap-overlay-dir overlay-dir)
      (wrap-content-type)
      (wrap-stacktrace)
      (http/run-server {:port (or port 6899)})))
