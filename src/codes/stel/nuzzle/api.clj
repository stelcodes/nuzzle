(ns codes.stel.nuzzle.api
  (:require [babashka.fs :as fs]
            [codes.stel.nuzzle.generator :as gen]
            [codes.stel.nuzzle.ring :as ring]
            [stasis.core :as stasis]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :as log]))

(defn inspect
  "Allows the user to inspect the site-config after modifications such as the
  drafts being optionally removed, the group and tag index pages being added,
  and :uri and :render-resource fields being added."
  [{:keys [site-config remove-drafts?]}]
  (log/info "🔍🐈 Creating realized site config for inspection")
  (when remove-drafts? (log/info "❌🐈 Removing drafts"))
  (-> site-config
      (gen/load-site-config)
      (gen/realize-site-config remove-drafts?)))

(defn export
  "Exports the website to :target-dir. The :static-dir is overlayed on top of
  the :target-dir after the web pages have been exported."
  [{:keys [site-config remove-drafts? static-dir target-dir render-page rss-opts]
    :or {target-dir "dist"} :as global-config}]
  {:pre [(or (map? global-config) (string? global-config))
         (string? static-dir)
         (string? target-dir)
         (or (nil? rss-opts) (map? rss-opts))]}
  (log/info "🔨🐈 Exporting static site to:" target-dir)
  (when remove-drafts? (log/info "❌🐈 Removing drafts"))
  (when static-dir (log/info "💎🐈 Using static asset directory:" static-dir))
  (let [realized-site-config
        (-> site-config
            (gen/load-site-config)
            (gen/realize-site-config remove-drafts?))
        rss-filename (or (:filename rss-opts) "rss.xml")
        rss-file (fs/file target-dir rss-filename)
        rss-feed (gen/create-rss-feed realized-site-config rss-opts)]
    (-> realized-site-config
        (gen/generate-page-list)
        (gen/generate-site-index render-page false)
        (gen/export-site-index static-dir target-dir))
    (when rss-feed
      (log/info "📰🐈 Creating RSS file:" rss-filename)
      (spit rss-file rss-feed)))
  (log/info "✅🐈 Export successful"))

(defn start-server
  "Starts a server using http-kit for development."
  [{:keys [static-dir dev-port remove-drafts? render-page site-config]
    :or {dev-port 5868}}]
  (log/info (str "✨🐈 Starting development server on port " dev-port))
  (when remove-drafts? (log/info "❌🐈 Removing drafts"))
  (when static-dir (log/info "💎🐈 Using static asset directory:" static-dir))
  (let [create-index #(-> site-config
                          (gen/load-site-config)
                          (gen/realize-site-config remove-drafts?)
                          (gen/generate-page-list)
                          (gen/generate-site-index render-page true))]
    (-> (stasis/serve-pages create-index)
        (ring/wrap-static-dir static-dir)
        (wrap-content-type)
        (wrap-stacktrace)
        (run-server {:port dev-port}))))
