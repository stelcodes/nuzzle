(ns nuzzle.schemas
  (:require
   [clojure.spec.alpha :as s]
   [spell-spec.alpha :as spell]))

(def http-url? #(re-find #"^https?://" %))

;; Page map keys
(s/def :nuzzle/title string?)
(s/def :nuzzle/render-content fn?)
(s/def :nuzzle/feed? boolean?)
(s/def :nuzzle/updated inst?)
(s/def :nuzzle/tags (s/coll-of keyword? :kind set?))
(s/def :nuzzle/draft? boolean?)
(s/def :nuzzle/render-page fn?)
(s/def :nuzzle/page
  (spell/keys :req [:nuzzle/title :nuzzle/render-page]
              :opt [:nuzzle/tags :nuzzle/render-content :nuzzle/updated :nuzzle/feed?
                    :nuzzle/draft? :nuzzle/summary :nuzzle/subtitle]))

(s/def :nuzzle.author/name string?)
(s/def :nuzzle.author/email string?)
(s/def :nuzzle.author/url http-url?)
(s/def :nuzzle/author
  (spell/keys :req-un [:nuzzle.author/name]
              :opt-un [:nuzzle.author/email :nuzzle.author/url]))

;; Syntax highlighter keys
;; (s/def :nuzzle.syntax-highlighter/provider #{:chroma :pygments})
;; (s/def :nuzzle.syntax-highlighter/style string?)
;; (s/def :nuzzle.syntax-highlighter/line-numbers? boolean?)


;; Atom feed keys
;; (s/def :nuzzle.atom-feed/title string?)
;; (s/def :nuzzle.atom-feed/subtitle string?)
;; (s/def :nuzzle.atom-feed/logo string?)
;; (s/def :nuzzle.atom-feed/icon string?)

;; Config keys
;; (s/def :nuzzle/base-url http-url?)
;; (s/def :nuzzle/syntax-highlighter
;;   (spell/keys :req-un [:nuzzle.syntax-highlighter/provider]
;;               :opt-un [:nuzzle.syntax-highlighter/style :nuzzle.syntax-highlighter/line-numbers?]))
;; (s/def :nuzzle/atom-feed
;;   (spell/keys :req-un [:nuzzle.atom-feed/title]
;;               :opt-un [:nuzzle.atom-feed/subtitle :nuzzle/author :nuzzle.atom-feed/logo :nuzzle.atom-feed/icon]))
;; (s/def :nuzzle/sitemap? boolean?)
;; (s/def :nuzzle/publish-dir string?)

;; Config Rules
(s/def :nuzzle/url (s/coll-of keyword? :kind vector?))

;; Whole config
(s/def :nuzzle/user-pages
  (s/map-of :nuzzle/url :nuzzle/page))

;; Might use this later for function instrumentation
;; (s/def :nuzzle/server-port (s/int-in 1024 65536))

(comment
 (s/explain
  :nuzzle/user-config
  {:nuzzle/render-page 'views.render-page
   ;; :nuzzle/build-drafts? nil
   ;; :nuzzle/server-port 5
   [:blog-posts :test-post] {:nuzzle/title "hi" :nuzzle/feed? true :nuzzle/updated "2022-07-19" :nuzzle/tags #{:hi}}}))
