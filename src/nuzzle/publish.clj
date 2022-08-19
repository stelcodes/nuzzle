(ns nuzzle.publish
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [nuzzle.config :as conf]
   [nuzzle.log :as log]
   [nuzzle.feed :as feed]
   [nuzzle.util :as util]
   [stasis.core :as stasis]
   [xmlns.http%3A%2F%2Fwww.sitemaps.org%2Fschemas%2Fsitemap%2F0.9 :as-alias sm]))

(defn create-sitemap
  "Assumes config is transformed and drafts have already been removed from
  rendered-site-index."
  ;; http://www.sitemaps.org/protocol.html
  [{:nuzzle/keys [base-url] :as config} rendered-site-index]
   (xml/emit-str
    {:tag ::sm/urlset
     :content
     (for [[rel-url _hiccup] rendered-site-index
           :let [page-key (util/url->page-key rel-url)
                 {:nuzzle/keys [content updated]} (get config page-key)
                 abs-url (str base-url rel-url)]]
       {:tag ::sm/url
        :content
        [{:tag ::sm/loc
          :content [abs-url]}
         (when (or updated content)
           {:tag ::sm/lastmod
            :content (str (or updated (util/path->last-mod-inst content)))})]})}
    {:encoding "UTF-8"}))

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
        sitemap-str (create-sitemap config rendered-site-index)]
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
