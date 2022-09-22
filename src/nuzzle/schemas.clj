(ns nuzzle.schemas
  (:require
   [clojure.spec.alpha :as s]
   [spell-spec.alpha :as spell]))

(def http-url? #(re-find #"^https?://" %))
(def http-url [:re #"^https?://"])
(def non-empty-string [:string {:max 1}])

(def vec-url [:vector keyword?])
(s/def :nuzzle/url (s/coll-of keyword? :kind vector?))
(def urlset [:set vec-url])
(s/def :nuzzle/urlset (s/coll-of :nuzzle/url :kind set?))

(s/def :nuzzle.author/name string?)
(s/def :nuzzle.author/email string?)
(s/def :nuzzle.author/url http-url?)
(def author [:map
             [:name non-empty-string]
             [:email {:optional true} non-empty-string]
             [:url {:optional true} http-url]])
(s/def :nuzzle/author
  (spell/keys :req-un [:nuzzle.author/name]
              :opt-un [:nuzzle.author/email :nuzzle.author/url]))

(s/def :nuzzle/title string?)
(s/def :nuzzle/render-content fn?)
(s/def :nuzzle/feed? boolean?)
(s/def :nuzzle/updated inst?)
(def tags [:set keyword?])
(s/def :nuzzle/tags (s/coll-of keyword? :kind set?))
(s/def :nuzzle/draft? boolean?)
(def index [:or urlset [:= :children]])
(s/def :nuzzle/index (s/or :urlset :nuzzle/urlset :children-literal #(= :children %)))
(s/def :nuzzle/render-page fn?)

;; A single page map
(def page
  [:map
   [:nuzzle/title non-empty-string]
   [:nuzzle/render-page fn?]
   [:nuzzle/render-content {:optional true} fn?]
   [:nuzzle/feed? {:optional true} boolean?]
   [:nuzzle/updated {:optional true} inst?]
   [:nuzzle/tags {:optional true} tags]
   [:nuzzle/draft? {:optional true} boolean?]
   [:nuzzle/index {:optional true} index]
   [:nuzzle/summary {:optional true} non-empty-string]
   [:nuzzle/subtitle {:optional true} non-empty-string]])
(s/def :nuzzle/page
  (spell/keys :req [:nuzzle/title :nuzzle/render-page]
              :opt [:nuzzle/tags :nuzzle/render-content :nuzzle/updated :nuzzle/feed?
                    :nuzzle/draft? :nuzzle/summary :nuzzle/subtitle :nuzzle/index :nuzzle/author]))

;; Whole pages map
(def pages [:and [:map-of vec-url page]
            [:fn {:error/message "Pages map must have homepage key []"}
             (fn [pages] (contains? pages []))]])
(s/def :nuzzle/user-pages
  (s/map-of :nuzzle/url :nuzzle/page))

(def tag-pages-opts
  [:map
   [:render-page fn?]
   [:create-title fn?]
   [:parent-url vec-url]])

(def load-pages-opts
  [:map
   [:tag-pages tag-pages-opts]
   [:remove-drafts? boolean?]])

(def dir-snapshot
  [:map-of string? string?])

(def dir-diff
  [:map
   [:added [:set string?]]
   [:removed [:set string?]]
   [:changed [:set string?]]])

(def regex #(= java.util.regex.Pattern (type %)))
