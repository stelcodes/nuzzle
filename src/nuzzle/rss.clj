(ns nuzzle.rss
  (:require
   [clj-rss.core :as rss]
   [nuzzle.log :as log]))

;; https://github.com/yogthos/clj-rss
;; http://validator.w3.org/feed/#validate_by_input
;; http://cyber.law.harvard.edu/rss/rss.html

(def valid-item-tags
  [:type :image :url :title :link :description "content:encoded" :author
   :category :comments :enclosure :guid :pubDate :source])

(def required-item-tags [:title :description])

(def valid-channel-tags
  [:title :link :feed-url :description :category :cloud :copyright :docs :image
   :language :lastBuildDate :managingEditor :pubDate :rating :skipDays :skipHours
   :ttl :webMaster])

(def required-channel-tags [:title :link :description])

(defn create-rss-feed
  "Creates a string of XML that is a valid RSS feed"
  ;; TODO: make sure that clj-rss baked in PermaLink=false is ok
  [{:nuzzle/keys [rss-channel] :keys [site-data] :as _config}]
  {:pre [(map? site-data) (map? rss-channel)]
   :post [(string? %)]}
  (let [{:keys [link]} rss-channel
        rss-items
        (for [{:nuzzle/keys [url] :keys [rss?] :as page} (vals site-data)
              :when rss?]
          (-> page
              (select-keys valid-item-tags)
              (assoc :guid (str link url))))]
    (try (apply rss/channel-xml
                rss-channel
                rss-items)
      (catch Exception e
        (let [msg (.getMessage e)]
          (log/error "Unable to create RSS feed.")
          (when (re-find #"is a required element$" msg)
            (log/error "The :nuzzle/rss-channel map must contain all of these keys:"
                       required-channel-tags))
          (when (re-find #"^unrecognized tags in channel" msg)
            (log/error "The :nuzzle/rss-channel map can only contain these keys: "
                       valid-channel-tags))
          (when (re-find #"^item" msg)
            (log/error "A page marked with :rss? true must have at least one of these keys:"
                       required-item-tags)))
        (throw e)))))
