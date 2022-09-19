(ns nuzzle.schemas
  (:require
   [clojure.spec.alpha :as s]
   [spell-spec.alpha :as spell]))

(def http-url? #(re-find #"^https?://" %))

(s/def :nuzzle/url (s/coll-of keyword? :kind vector?))
(s/def :nuzzle/urlset (s/coll-of :nuzzle/url :kind set?))

(s/def :nuzzle.author/name string?)
(s/def :nuzzle.author/email string?)
(s/def :nuzzle.author/url http-url?)
(s/def :nuzzle/author
  (spell/keys :req-un [:nuzzle.author/name]
              :opt-un [:nuzzle.author/email :nuzzle.author/url]))

(s/def :nuzzle/title string?)
(s/def :nuzzle/render-content fn?)
(s/def :nuzzle/feed? boolean?)
(s/def :nuzzle/updated inst?)
(s/def :nuzzle/tags (s/coll-of keyword? :kind set?))
(s/def :nuzzle/draft? boolean?)
(s/def :nuzzle/index (s/or :urlset :nuzzle/urlset :children-literal #(= :children %)))
(s/def :nuzzle/render-page fn?)

;; A single page map
(s/def :nuzzle/page
  (spell/keys :req [:nuzzle/title :nuzzle/render-page]
              :opt [:nuzzle/tags :nuzzle/render-content :nuzzle/updated :nuzzle/feed?
                    :nuzzle/draft? :nuzzle/summary :nuzzle/subtitle :nuzzle/index :nuzzle/author]))

;; Whole pages map
(s/def :nuzzle/user-pages
  (s/map-of :nuzzle/url :nuzzle/page))
