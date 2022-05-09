(ns nuzzle.export
  (:require
   [babashka.fs :as fs]
   [nuzzle.generator :as gen]
   [nuzzle.log :as log]
   [nuzzle.rss :as rss]
   [nuzzle.util :as util]
   [stasis.core :as stasis]))

(defn export-rss
  [{:keys [export-dir] :as config}]
  (let [rss-file (fs/file export-dir "feed.xml")
        _ (log/log-rss rss-file)
        rss-feed (rss/create-rss-feed config)]
    (spit rss-file rss-feed)))

(defn export-site
  [{:keys [overlay-dir export-dir rss-channel] :as config}]
  (log/log-export-start export-dir)
  (fs/create-dirs export-dir)
  (stasis/empty-directory! export-dir)
  (-> config
      (gen/generate-site-index false)
      (stasis/export-pages export-dir))
  (when overlay-dir
    (log/log-overlay-dir overlay-dir)
    (util/ensure-overlay-dir overlay-dir)
    (fs/copy-tree overlay-dir export-dir))
  (when rss-channel
    (export-rss config))
  (log/log-export-end))
