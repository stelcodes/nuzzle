(ns nuzzle.server
  (:require
   [clojure.java.browse :as browse]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [io.aviso.exception :as except]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.pages :as pages]
   [nuzzle.schemas :as schemas]
   [nuzzle.util :as util]
   [org.httpkit.server :as http]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.file :refer [file-request]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-web]]))

(defn wrap-stacktrace-log
  {:malli/schema [:-> fn? fn?]}
  [app]
  (fn [request]
    (try (app request)
      (catch Throwable e
        (log/error (.getMessage e))
        (except/write-exception e)
        (throw e)))))

(defn wrap-overlay-dir
  {:malli/schema [:-> fn? string? fn?]}
  [app overlay-dir]
  (if overlay-dir
    (fn [request]
      (or (file-request request overlay-dir) (app request)))
    (fn [request] (app request))))

(defn load-livejs
  {:malli/schema [:-> int? string?]}
  [refresh-interval]
  (let [script (-> "nuzzle/js/livejs.js" io/resource slurp)]
    (cond-> script
      (and refresh-interval (not= 2500 refresh-interval))
      (str/replace-first "2500" (str refresh-interval)))))

(defn handle-page-request
  "This handler is responsible for creating an HTML document for a page when
  it's located in the page map. Otherwise return a 404 Not Found"
  {:malli/schema [:-> schemas/alt-pages [:? schemas/handle-page-request-opts] fn?]}
  [pages & {:keys [remove-drafts refresh-interval]}]
  (let [livejs-script (when refresh-interval
                        [:script {:type "text/javascript"}
                         (-> refresh-interval load-livejs hiccup/raw-html)])]
    (fn [{:keys [uri] :as _request}]
      (let [loaded-pages (pages/load-pages pages {:remove-drafts remove-drafts})
            {:nuzzle/keys [render-page url] :as page} (loaded-pages (util/vectorize-url uri))]
        (if page
          (do (log/log-rendering-page url)
            {:status 200
             :body (cond-> page
                     true render-page
                     livejs-script (hiccup/transform-hiccup
                                    {:body #(conj % livejs-script)})
                     true hiccup/hiccup->html-document)
             :headers {"Content-Type" "text/html"}})
          {:status 404
           :body "<h1>Page Not Found</h1>"
           :headers {"Content-Type" "text/html"}})))))

(defn start-server
  {:malli/schema [:-> schemas/alt-pages [:? schemas/serve-opts] fn?]}
  [pages & {:keys [port overlay-dir remove-drafts refresh-interval open-browser]
            :or {port 6899}}]
  (log/log-site-server port)
  (when overlay-dir
    (log/log-overlay-dir overlay-dir)
    (util/ensure-overlay-dir overlay-dir))
  (let [stop-fn (-> (handle-page-request pages
                                         {:remove-drafts remove-drafts
                                          :refresh-interval refresh-interval})
                    (wrap-overlay-dir overlay-dir)
                    (wrap-content-type)
                    (wrap-stacktrace-log)
                    (wrap-stacktrace-web)
                    (http/run-server {:port port}))]
    (when open-browser (browse/browse-url (str "http://localhost:" port)))
    stop-fn))
