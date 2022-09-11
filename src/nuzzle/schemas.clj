(ns nuzzle.schemas
  (:require
   [clojure.spec.alpha :as s]
   [spell-spec.alpha :as spell]))

(def date-str? #(try (java.time.LocalDate/parse %) (catch Throwable _ nil)))
(def datetime-str? #(try (java.time.LocalDateTime/parse %) (catch Throwable _ nil)))
(def zoned-datetime-str? #(try (java.time.ZonedDateTime/parse %) (catch Throwable _ nil)))

(def http-url? #(re-find #"^https?://" %))

;; Page map keys
(s/def :nuzzle/title string?)
(s/def :nuzzle/feed? boolean?)
(s/def :nuzzle/updated (s/or :date date-str? :datetime datetime-str? :zoned-datetime zoned-datetime-str?))
(s/def :nuzzle/tags (s/coll-of keyword? :kind set?))
(s/def :nuzzle/draft? boolean?)

(s/def :nuzzle.author/name string?)
(s/def :nuzzle.author/email string?)
(s/def :nuzzle.author/url http-url?)
(s/def :nuzzle/author
  (spell/keys :req-un [:nuzzle.author/name]
              :opt-un [:nuzzle.author/email :nuzzle.author/url]))

;; Syntax highlighter keys
(s/def :nuzzle.syntax-highlighter/provider #{:chroma :pygments})
(s/def :nuzzle.syntax-highlighter/style string?)
(s/def :nuzzle.syntax-highlighter/line-numbers? boolean?)


;; Atom feed keys
(s/def :nuzzle.atom-feed/title string?)
(s/def :nuzzle.atom-feed/subtitle string?)
(s/def :nuzzle.atom-feed/logo string?)
(s/def :nuzzle.atom-feed/icon string?)

;; Config keys
(s/def :nuzzle/render-page fn?)
;; (s/def :nuzzle/base-url http-url?)
(s/def :nuzzle/syntax-highlighter
  (spell/keys :req-un [:nuzzle.syntax-highlighter/provider]
              :opt-un [:nuzzle.syntax-highlighter/style :nuzzle.syntax-highlighter/line-numbers?]))
(s/def :nuzzle/atom-feed
  (spell/keys :req-un [:nuzzle.atom-feed/title]
              :opt-un [:nuzzle.atom-feed/subtitle :nuzzle/author :nuzzle.atom-feed/logo :nuzzle.atom-feed/icon]))
(s/def :nuzzle/sitemap? boolean?)
;; (s/def :nuzzle/publish-dir string?)
(s/def :nuzzle/build-drafts? boolean?)
(s/def :nuzzle/custom-elements (s/map-of keyword? symbol?))

;; Config Rules
(s/def :nuzzle/page-key (s/coll-of keyword? :kind vector?))
(s/def :nuzzle/page-map (spell/keys :req [:nuzzle/title]
                                    :opt [:nuzzle/tags :nuzzle/updated :nuzzle/feed? :nuzzle/draft? :nuzzle/summary :nuzzle/subtitle]))
(s/def :nuzzle/config-entry (s/or :page (s/tuple :nuzzle/page-key :nuzzle/page-map)
                                  :option (s/tuple keyword? any?)))

;; Whole config
(s/def :nuzzle/user-config
  (s/and
   (spell/keys :req [:nuzzle/render-page]
               :opt [:nuzzle/syntax-highlighter :nuzzle/atom-feed :nuzzle/build-drafts?
                     :nuzzle/sitemap? :nuzzle/custom-elements])
   (s/every :nuzzle/config-entry)))

;; Might use this later for function instrumentation
;; (s/def :nuzzle/server-port (s/int-in 1024 65536))

(comment
 (s/explain
  :nuzzle/user-config
  {:nuzzle/render-page 'views.render-page
   ;; :nuzzle/build-drafts? nil
   ;; :nuzzle/server-port 5
   [:blog-posts :test-post] {:nuzzle/title "hi" :nuzzle/feed? true :nuzzle/updated "2022-07-19" :nuzzle/tags #{:hi}}}))
