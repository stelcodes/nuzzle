(ns nuzzle.api
  (:require [babashka.fs :as fs]
            [nuzzle.config :as conf]
            [nuzzle.generator :as gen]
            [nuzzle.log :as log]
            [nuzzle.ring :as ring]
            [nuzzle.rss :as rss]
            [nuzzle.util :as util]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [org.httpkit.server :as http]))

(defn realize
  "Allows the user to visualize the site data after Nuzzle's modifications."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (let [{:keys [remove-drafts?] :as config}
        (conf/load-config config-overrides)]
    (log/info "🔍🐈 Printing realized site data for inspection")
    (when remove-drafts? (log/info "❌🐈 Removing drafts"))
    (-> config
        (gen/realize-site-data)
        (util/convert-site-data-to-vector))))

(defn export
  "Exports the website to :export-dir. The :overlay-dir is overlayed on top of
  the :export-dir after the web pages have been exported."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (let [{:keys [rss-channel overlay-dir remove-drafts? export-dir render-webpage] :as config}
        (conf/load-config config-overrides)
        realized-site-data (gen/realize-site-data config)
        rss-file (fs/file export-dir "rss.xml")
        rss-feed (rss/create-rss-feed realized-site-data rss-channel)]
    (log/info "🔨🐈 Exporting static site to:" export-dir)
    (when remove-drafts? (log/info "❌🐈 Removing drafts"))
    (when overlay-dir (log/info "💎🐈 Using overlay directory:" overlay-dir))
    (-> realized-site-data
        (gen/generate-page-list)
        (gen/generate-site-index render-webpage false)
        (gen/export-site-index overlay-dir export-dir))
    (when rss-feed
      (log/info "📰🐈 Creating RSS file:" (fs/canonicalize rss-file))
      (spit rss-file rss-feed))
    (log/info "✅🐈 Export successful")))

(defn serve
  "Starts a server using http-kit for development."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (let [{:keys [remove-drafts? dev-port] :as config}
        (conf/load-config config-overrides)]
    (log/info (str "✨🐈 Starting development server on port " dev-port))
    (when remove-drafts? (log/info "❌🐈 Removing drafts"))
    (-> (ring/wrap-serve-pages config-overrides)
        (ring/wrap-overlay-dir config-overrides)
        (wrap-content-type)
        (wrap-stacktrace)
        (http/run-server {:port (:dev-port config)}))))
