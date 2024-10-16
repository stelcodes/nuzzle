(ns nuzzle.schemas
  (:require
   [clojure.spec.alpha :as s]
   [malli.util :as mu]
   [spell-spec.alpha :as spell]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clojure.spec.alpha schemas
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
(s/def :nuzzle/feed boolean?)
(s/def :nuzzle/updated inst?)
(s/def :nuzzle/published inst?)
(s/def :nuzzle/tags (s/coll-of keyword? :kind set?))
(s/def :nuzzle/draft boolean?)
(s/def :nuzzle/index (s/or :urlset :nuzzle/urlset :children-literal #(= :children %)))
(s/def :nuzzle/render-page fn?)

(s/def :nuzzle/page
  (spell/keys :req [:nuzzle/title :nuzzle/render-page]
              :opt [:nuzzle/tags :nuzzle/render-content :nuzzle/updated :nuzzle/feed :nuzzle/published
                    :nuzzle/draft :nuzzle/summary :nuzzle/subtitle :nuzzle/index :nuzzle/author]))

(s/def :nuzzle/user-pages
  (s/map-of :nuzzle/url :nuzzle/page))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; malli schemas

(def http-url [:re #"^https?://"])

(def vec-url [:vector keyword?])

(def urlset [:set vec-url])
(def author [:map
             [:name string?]
             [:email {:optional true} [:maybe string?]]
             [:url {:optional true} [:maybe http-url]]])
(def tags [:set keyword?])

;; A single page map
(def page
  [:map
   [:nuzzle/title string?]
   [:nuzzle/render-page fn?]
   [:nuzzle/render-content {:optional true} [:maybe fn?]]
   [:nuzzle/feed {:optional true} [:maybe boolean?]]
   [:nuzzle/updated {:optional true} [:maybe inst?]]
   [:nuzzle/published {:optional true} [:maybe inst?]]
   [:nuzzle/tags {:optional true} [:maybe tags]]
   [:nuzzle/draft {:optional true} [:maybe boolean?]]
   [:nuzzle/index {:optional true} [:maybe urlset]]
   [:nuzzle/author {:optional true} [:maybe author]]
   [:nuzzle/summary {:optional true} [:maybe string?]]
   [:nuzzle/subtitle {:optional true} [:maybe string?]]])

(def validate-page
  (mu/merge page [:map [:nuzzle/url vec-url] [:nuzzle/title [:or :string fn?]]]))

(def enriched-page
  (mu/merge page
            [:map
             [:nuzzle/url vec-url]
             [:nuzzle/render-content fn?]
             [:nuzzle/get-pages fn?]]))

(def homepage-check
  [:fn {:error/message "Pages map must have homepage key []"}
   (fn [pages] (contains? pages []))])

(def enriched-pages
  [:and
   [:map-of vec-url enriched-page]
   homepage-check])

;; Whole pages map
(def pages
  [:and
   [:map-of vec-url page]
   homepage-check])

(def alt-pages [:alt pages [:fn var?] fn?])

(def load-pages-opts
  [:map {:closed true}
   [:remove-drafts {:optional true} [:maybe boolean?]]])

(def dir-snapshot
  [:map-of string? string?])

(def dir-diff
  [:map {:closed true}
   [:added [:set string?]]
   [:removed [:set string?]]
   [:changed [:set string?]]])

(def regex? [:fn (fn [x] (= java.util.regex.Pattern (type x)))])

(def atom-feed
  [:map {:closed true}
   [:title string?]
   [:author {:optional true} [:maybe author]]
   [:logo {:optional true} [:maybe string?]]
   [:icon {:optional true} [:maybe string?]]
   [:subtitle {:optional true} [:maybe string?]]])

(def publish-opts
  [:map {:closed true}
   [:base-url {:optional true} [:maybe http-url]]
   [:publish-dir {:optional true} [:maybe string?]]
   [:overlay-dir {:optional true} [:maybe string?]]
   [:remove-drafts {:optional true} [:maybe boolean?]]
   [:site-map? {:optional true} [:maybe boolean?]]
   [:atom-feed {:optional true} [:maybe atom-feed]]])

(def serve-opts
  [:map {:closed true}
   [:port {:optional true} [:maybe int?]]
   [:overlay-dir {:optional true} [:maybe string?]]
   [:remove-drafts {:optional true} [:maybe boolean?]]
   [:refresh-interval {:optional true} [:maybe int?]]])

(def handle-page-request-opts
  [:map {:closed true}
   [:remove-drafts {:optional true} [:maybe boolean?]]
   [:refresh-interval {:optional true} [:maybe int?]]])
