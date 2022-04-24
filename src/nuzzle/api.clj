(ns nuzzle.api
  (:require [babashka.fs :as fs]
            [nuzzle.config :as conf]
            [nuzzle.generator :as gen]
            [nuzzle.log :as log]
            [nuzzle.ring :as ring]
            [nuzzle.rss :as rss]
            [nuzzle.util :as util]
            [clojure.pprint :refer [pprint]]
            [stasis.core :as stasis]
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
  "Exports the website to :output-dir. The :static-dir is overlayed on top of
  the :output-dir after the web pages have been exported."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (let [{:keys [rss-channel static-dir remove-drafts? output-dir render-webpage] :as config}
        (conf/load-config config-overrides)
        realized-site-data (gen/realize-site-data config)
        rss-file (fs/file output-dir "rss.xml")
        rss-feed (rss/create-rss-feed realized-site-data rss-channel)]
    (log/info "🔨🐈 Exporting static site to:" output-dir)
    (when remove-drafts? (log/info "❌🐈 Removing drafts"))
    (when static-dir (log/info "💎🐈 Using static asset directory:" static-dir))
    (-> realized-site-data
        (gen/generate-page-list)
        (gen/generate-site-index render-webpage false)
        (gen/export-site-index static-dir output-dir))
    (when rss-feed
      (log/info "📰🐈 Creating RSS file:" (fs/canonicalize rss-file))
      (spit rss-file rss-feed))
    (log/info "✅🐈 Export successful")))

(defn start-server
  "Starts a server using http-kit for development."
  [& {:as config-overrides}]
  {:pre [(or (nil? config-overrides) (map? config-overrides))]}
  (let [{:keys [render-webpage remove-drafts? static-dir dev-port] :as config}
        (conf/load-config config-overrides)
        create-index #(-> (conf/load-config config-overrides)
                          (gen/realize-site-data)
                          (gen/generate-page-list)
                          (gen/generate-site-index render-webpage true))]
    (log/info (str "✨🐈 Starting development server on port " dev-port))
    (when remove-drafts? (log/info "❌🐈 Removing drafts"))
    (when static-dir (log/info "💎🐈 Using static asset directory:" static-dir))
    (-> (stasis/serve-pages create-index)
        (ring/wrap-static-dir static-dir)
        (wrap-content-type)
        (wrap-stacktrace)
        (http/run-server {:port (:dev-port config)}))))
