(ns nuzzle.server
  (:require
   [io.aviso.exception :as except]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.pages :as pages]
   [nuzzle.util :as util]
   [org.httpkit.server :as http]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.file :refer [file-request]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-web]]))

(defn wrap-stacktrace-log [app]
  (fn [request]
    (try (app request)
      (catch Throwable e
        (log/error (.getMessage e))
        (except/write-exception e)
        (throw e)))))

(defn wrap-overlay-dir
  [app overlay-dir]
  (if overlay-dir
    (fn [request]
      (or (file-request request overlay-dir) (app request)))
    (fn [request] (app request))))

(defn handle-page-request
  "This handler is responsible for creating an HTML document for a page when
  it's located in the page map. Otherwise return a 404 Not Found"
  [pages & {:keys [remove-drafts?]}]
  (fn [{:keys [uri] :as _request}]
    (let [loaded-pages (pages/load-pages pages :remove-drafts? remove-drafts?)
          {:nuzzle/keys [render-page url] :as page} (loaded-pages (util/vectorize-url uri))]
      (if page
        (do (log/log-rendering-page url)
          {:status 200
           :body (-> page render-page hiccup/hiccup->html-document)
           :headers {"Content-Type" "text/html"}})
        {:status 404
         :body "<h1>Page Not Found</h1>"
         :headers {"Content-Type" "text/html"}}))))

(defn start-server [pages & {:keys [port overlay-dir remove-drafts?] :or {port 6899}}]
  (log/log-site-server port)
  (when overlay-dir
    (log/log-overlay-dir overlay-dir)
    (util/ensure-overlay-dir overlay-dir))
  (-> (handle-page-request pages :remove-drafts? remove-drafts?)
      (wrap-overlay-dir overlay-dir)
      (wrap-content-type)
      (wrap-stacktrace-log)
      (wrap-stacktrace-web)
      (http/run-server {:port port})))
