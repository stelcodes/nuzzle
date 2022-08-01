(ns nuzzle.publish
  (:require
   [babashka.fs :as fs]
   [nuzzle.generator :as gen]
   [nuzzle.log :as log]
   [nuzzle.rss :as rss]
   [nuzzle.sitemap :as sitemap]
   [nuzzle.util :as util]
   [stasis.core :as stasis]))

(defn publish-rss
  [{:nuzzle/keys [publish-dir] :as config}]
  (let [rss-file (fs/file publish-dir "feed.xml")
        _ (log/log-rss rss-file)
        rss-feed (rss/create-rss-feed config)]
    (spit rss-file rss-feed)))

(defn publish-sitemap
  [{:nuzzle/keys [publish-dir] :as config} rendered-site-index]
  (let [sitemap-file (fs/file publish-dir "sitemap.xml")
        _ (log/log-sitemap sitemap-file)
        sitemap-str (sitemap/create-sitemap config rendered-site-index)]
    (spit sitemap-file sitemap-str)))

(defn publish-site
  [{:nuzzle/keys [overlay-dir publish-dir rss-channel sitemap?] :as config}]
  (let [rendered-site-index (gen/generate-rendered-site-index config)]
    (log/log-publish-start publish-dir)
    (fs/create-dirs publish-dir)
    (stasis/empty-directory! publish-dir)
    (stasis/export-pages rendered-site-index publish-dir)
    (when overlay-dir
      (log/log-overlay-dir overlay-dir)
      (util/ensure-overlay-dir overlay-dir)
      (fs/copy-tree overlay-dir publish-dir))
    (when rss-channel
      (publish-rss config))
    (when sitemap?
      (publish-sitemap config rendered-site-index))
    (log/log-publish-end)))
