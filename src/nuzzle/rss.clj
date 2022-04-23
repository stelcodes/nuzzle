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
  [realized-site-data {:keys [link] :as rss-opts}]
  {:pre [(map? realized-site-data) (map? rss-opts)]
   :post [(string? %)]}
  (let [rss-items
        (for [{:keys [rss? uri] :as webpage} (vals realized-site-data)
              :when rss?]
          (-> webpage
              (select-keys valid-item-tags)
              (assoc :guid (str link uri))))]
    (try (apply rss/channel-xml
                rss-opts
                rss-items)
      (catch Exception e
        (let [msg (.getMessage e)]
          (log/error "Unable to create RSS feed.")
          (when (re-find #"is a required element$" msg)
            (log/error "The :rss-opts map must contain all of these keys:"
                       required-channel-tags))
          (when (re-find #"^unrecognized tags in channel" msg)
            (log/error "The :rss-opts map can only contain these keys: "
                       valid-channel-tags))
          (when (re-find #"^item" msg)
            (log/error "A webpage marked with :rss? true must have at least one of these keys:"
                       required-item-tags)))
        (throw e)))))
