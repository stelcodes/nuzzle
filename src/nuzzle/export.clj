(ns nuzzle.export
  (:require
   [babashka.fs :as fs]
   [nuzzle.generator :as gen]
   [nuzzle.log :as log]
   [nuzzle.rss :as rss]
   [nuzzle.sitemap :as sitemap]
   [nuzzle.util :as util]
   [stasis.core :as stasis]))

(defn export-rss
  [{:keys [export-dir] :as config}]
  (let [rss-file (fs/file export-dir "feed.xml")
        _ (log/log-rss rss-file)
        rss-feed (rss/create-rss-feed config)]
    (spit rss-file rss-feed)))

(defn export-sitemap
  [{:keys [export-dir] :as config} rendered-site-index]
  (let [sitemap-file (fs/file export-dir "sitemap.xml")
        _ (log/log-sitemap sitemap-file)
        sitemap-str (sitemap/create-sitemap config rendered-site-index)]
    (spit sitemap-file sitemap-str)))

(defn export-site
  [{:keys [sitemap? overlay-dir export-dir rss-channel] :as config}]
  (let [rendered-site-index (gen/generate-rendered-site-index config)]
    (log/log-export-start export-dir)
    (fs/create-dirs export-dir)
    (stasis/empty-directory! export-dir)
    (stasis/export-pages rendered-site-index export-dir)
    (when overlay-dir
      (log/log-overlay-dir overlay-dir)
      (util/ensure-overlay-dir overlay-dir)
      (fs/copy-tree overlay-dir export-dir))
    (when rss-channel
      (export-rss config))
    (when sitemap?
      (export-sitemap config rendered-site-index))
    (log/log-export-end)))
