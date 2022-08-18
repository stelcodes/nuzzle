(ns nuzzle.publish
  (:require
   [babashka.fs :as fs]
   [nuzzle.config :as conf]
   [nuzzle.log :as log]
   [nuzzle.feed :as feed]
   [nuzzle.sitemap :as sitemap]
   [nuzzle.util :as util]
   [stasis.core :as stasis]))

(defn publish-atom-feed
  "The optional test-ops map can make build deterministic by setting
  :deterministic? true"
  [{:nuzzle/keys [publish-dir] :as config} rendered-site-index & {:as test-opts}]
  (let [feed-file (fs/file publish-dir "feed.xml")
        _ (log/log-feed feed-file)
        feed-str (feed/create-atom-feed config rendered-site-index test-opts)]
    (spit feed-file feed-str)))

(defn publish-sitemap
  [{:nuzzle/keys [publish-dir] :as config} rendered-site-index]
  (let [sitemap-file (fs/file publish-dir "sitemap.xml")
        _ (log/log-sitemap sitemap-file)
        sitemap-str (sitemap/create-sitemap config rendered-site-index)]
    (spit sitemap-file sitemap-str)))

(defn publish-site
  "The optional test-ops map can make build deterministic by setting
  :deterministic? true"
  [{:nuzzle/keys [overlay-dir publish-dir atom-feed sitemap?] :as config} & {:as test-opts}]
  (let [rendered-site-index (conf/create-site-index config)]
    (log/log-publish-start publish-dir)
    (fs/create-dirs publish-dir)
    (stasis/empty-directory! publish-dir)
    (stasis/export-pages rendered-site-index publish-dir)
    (when overlay-dir
      (log/log-overlay-dir overlay-dir)
      (util/ensure-overlay-dir overlay-dir)
      (fs/copy-tree overlay-dir publish-dir))
    (when atom-feed
      (publish-atom-feed config rendered-site-index test-opts))
    (when sitemap?
      (publish-sitemap config rendered-site-index))
    (log/log-publish-end)))
