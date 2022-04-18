(ns codes.stel.nuzzle.api
  (:require [babashka.fs :as fs]
            [codes.stel.nuzzle.generator :as gen]
            [codes.stel.nuzzle.log :as log]
            [codes.stel.nuzzle.ring :as ring]
            [stasis.core :as stasis]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [org.httpkit.server :as http]))

(defn inspect
  "Allows the user to inspect the site-data after modifications such as the
  drafts being optionally removed, the group and tag index pages being added,
  and :uri and :render-resource fields being added."
  [{:keys [site-data remove-drafts?] :or {remove-drafts? false}}]
  (log/info "🔍🐈 Creating realized site data for inspection")
  (when remove-drafts? (log/info "❌🐈 Removing drafts"))
  (-> site-data
      (gen/load-site-data)
      (gen/realize-site-data remove-drafts?)))

(defn export
  "Exports the website to :target-dir. The :static-dir is overlayed on top of
  the :target-dir after the web pages have been exported."
  [{:keys [site-data remove-drafts? static-dir target-dir render-webpage rss-opts]
    :or {target-dir "dist" remove-drafts? false} :as nuzzle-config}]
  {:pre [(map? nuzzle-config)
         (string? site-data)
         (or (nil? static-dir) (string? static-dir))
         (fn? render-webpage)
         (string? target-dir)
         (or (nil? rss-opts) (map? rss-opts))]}
  (log/info "🔨🐈 Exporting static site to:" target-dir)
  (when remove-drafts? (log/info "❌🐈 Removing drafts"))
  (when static-dir (log/info "💎🐈 Using static asset directory:" static-dir))
  (let [realized-site-data
        (-> site-data
            (gen/load-site-data)
            (gen/realize-site-data remove-drafts?))
        rss-filename (or (:filename rss-opts) "rss.xml")
        rss-file (fs/file target-dir rss-filename)
        rss-feed (gen/create-rss-feed realized-site-data rss-opts)]
    (-> realized-site-data
        (gen/generate-page-list)
        (gen/generate-site-index render-webpage false)
        (gen/export-site-index static-dir target-dir))
    (when rss-feed
      (log/info "📰🐈 Creating RSS file:" rss-filename)
      (spit rss-file rss-feed)))
  (log/info "✅🐈 Export successful"))

(defn start-server
  "Starts a server using http-kit for development."
  [{:keys [static-dir dev-port remove-drafts? render-webpage site-data]
    :or {dev-port 5868 remove-drafts? false} :as nuzzle-config}]
  {:pre [(map? nuzzle-config)
         (string? site-data)
         (or (nil? static-dir) (string? static-dir))
         (fn? render-webpage)
         (int? dev-port)]}
  (log/info (str "✨🐈 Starting development server on port " dev-port))
  (when remove-drafts? (log/info "❌🐈 Removing drafts"))
  (when static-dir (log/info "💎🐈 Using static asset directory:" static-dir))
  (let [create-index #(-> site-data
                          (gen/load-site-data)
                          (gen/realize-site-data remove-drafts?)
                          (gen/generate-page-list)
                          (gen/generate-site-index render-webpage true))]
    (-> (stasis/serve-pages create-index)
        (ring/wrap-static-dir static-dir)
        (wrap-content-type)
        (wrap-stacktrace)
        (http/run-server {:port dev-port}))))
