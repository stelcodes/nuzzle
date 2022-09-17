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
      (or (file-request request overlay-dir) (app request)))
    (fn [request] (app request))))

(defn handle-page-request
  "Handler that wraps around stasis.core/serve-pages, if config is a var then
  the config is resolved and validated upon each request. Otherwise the config
  is validated once and the app is built just once."
  [pages & {:keys [remove-drafts?]}]
  (fn [request]
    (let [loaded-pages (pages/load-pages pages :remove-drafts? remove-drafts?)
          stasis-pages (pages/create-stasis-pages loaded-pages)
          app (stasis/serve-pages stasis-pages)]
      (app request))))

(defn start-server [pages & {:keys [port overlay-dir remove-drafts?] :or {port 6899}}]
  (log/log-start-server port)
  (when overlay-dir
    (log/log-overlay-dir overlay-dir)
    (util/ensure-overlay-dir overlay-dir))
  (-> (handle-page-request pages :remove-drafts? remove-drafts?)
      (wrap-overlay-dir overlay-dir)
      (wrap-content-type)
      (wrap-stacktrace)
      (http/run-server {:port port})))
