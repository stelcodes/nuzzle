(ns nuzzle.schemas
  (:require
   [clojure.spec.alpha :as s]
   [spell-spec.alpha :as spell]))

(def datetime? #(try (java.time.LocalDate/parse %) true (catch Throwable _ false)))

;; Page map keys
(s/def :nuzzle/title string?)
(s/def :nuzzle/rss? boolean?)
(s/def :nuzzle/modified datetime?)
(s/def :nuzzle/tags (s/coll-of keyword? :kind set?))
(s/def :nuzzle/draft? boolean?)

;; Syntax highlighter keys
(s/def :nuzzle.syntax-highlighter/provider #{:chroma :pygments})
(s/def :nuzzle.syntax-highlighter/style string?)
(s/def :nuzzle.syntax-highlighter/line-numbers? boolean?)

;; Config keys
(s/def :nuzzle/render-page symbol?)
(s/def :nuzzle/base-url #(re-find #"^https?://" %))
(s/def :nuzzle/syntax-highlighter
  (spell/keys :req-un [:nuzzle.syntax-highlighter/provider]
              :opt-un [:nuzzle.syntax-highlighter/style :nuzzle.syntax-highlighter/line-numbers?]))
(s/def :nuzzle/rss-channel
  (spell/keys :req-un [:nuzzle.rss/title :nuzzle.rss/link :nuzzle.rss/description]))
(s/def :nuzzle/sitemap? boolean?)
(s/def :nuzzle/server-port (s/int-in 1024 65536))
(s/def :nuzzle/overlay-dir string?)
(s/def :nuzzle/publish-dir string?)
(s/def :nuzzle/build-drafts? boolean?)
(s/def :nuzzle/custom-elements (s/map-of keyword? symbol?))

;; Config Rules
(s/def :nuzzle/page-key (s/coll-of keyword? :kind vector?))
(s/def :nuzzle/page-map (spell/keys :req [:nuzzle/title]
                                    :opt [:nuzzle/tags :nuzzle/modified :nuzzle/rss? :nuzzle/draft?]))
(s/def :nuzzle/config-entry (s/or :page (s/tuple :nuzzle/page-key :nuzzle/page-map)
                                  :option (s/tuple keyword? any?)))

;; Whole config
(s/def :nuzzle/user-config
  (s/and
   (spell/keys :req [:nuzzle/base-url :nuzzle/render-page]
               :opt [:nuzzle/syntax-highlighter :nuzzle/rss-channel :nuzzle/build-drafts?
                     :nuzzle/sitemap? :nuzzle/custom-elements :nuzzle/publish-dir :nuzzle/overlay-dir])
   (s/every :nuzzle/config-entry)))

(comment
 (s/explain
  :nuzzle/user-config
  {:nuzzle/base-url "https://test.com"
   :nuzzle/render-page 'views.render-page
   ;; :nuzzle/build-drafts? nil
   ;; :nuzzle/server-port 5
   [:blog-posts :test-post] {:nuzzle/title "hi" :nuzzle/rss? true :nuzzle/modified "2022-07-19" :nuzzle/tags #{:hi}}}))
