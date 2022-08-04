(ns nuzzle.schemas
  (:require
   [clojure.spec.alpha :as s]))

(def local-date
  [:fn {:error/message "should be a date string in format YYYY-MM-DD"
        :decode/local-date #(try (java.time.LocalDate/parse %)
                              (catch Exception _ %))}
   #(= java.time.LocalDate (class %))])

(def id [:or [:vector keyword?] keyword?])

(def site-data
  [:set {:min 1}
   [:and
    [:map
     [:id id]
     [:modified {:optional true} local-date]
     [:tags {:optional true}
      [:set keyword?]]
     [:index {:optional true}
      [:set id]]]
    [:fn {:error/message ":site-data map with {:rss? true} needs a :title or :description"}
     (fn [{:keys [rss? title description]}]
       (or (not rss?) (or title description)))]]])

(def syntax-highlighter
  [:map
   ;; TODO: Add option to specify custom highlighting command
   [:provider [:enum :chroma :pygments]]
   [:style {:optional true} [:or :string :nil]]
   [:line-numbers? {:optional true} :boolean]])

(def base-url
  [:and
   :string
   [:re {:error/message ":nuzzle/base-url must start with http:// or https://"}
    #"^https?://"]])

(def config
  [:map
   {:closed true}
   [:site-data site-data]
   [:nuzzle/render-page fn?]
   [:nuzzle/base-url base-url]
   [:nuzzle/syntax-highlighter {:optional true} syntax-highlighter]
   [:nuzzle/custom-elements {:optional true} [:map-of :keyword :symbol]]
   [:nuzzle/overlay-dir {:optional true} string?]
   [:nuzzle/publish-dir {:optional true} string?]
   [:nuzzle/rss-channel {:optional true} [:map {:closed true}
                                   [:title string?]
                                   [:link string?]
                                   [:description string?]]]
   [:nuzzle/sitemap? {:optional true} :boolean]
   [:nuzzle/build-drafts? {:optional true} boolean?]
   [:nuzzle/server-port {:optional true} [:and int? [:> 1023] [:< 65536]]]])

;; clojure.spec ideas
(comment
 (def datetime? #(try (java.time.LocalDate/parse %) true (catch Throwable _ false)))
 (s/def :nuzzle/title string?)
 (s/def :nuzzle/rss? boolean?)
 (s/def :nuzzle/modified datetime?)
 (s/def :nuzzle/page-key (s/coll-of keyword? :kind vector?))
 (s/def :nuzzle/page-val (s/keys :req [:nuzzle/title] :opt [:nuzzle/rss? :nuzzle/modified]))
 (s/def :nuzzle/config-entry (s/or :page (s/tuple :nuzzle/page-key :nuzzle/page-val)
                                   :option (s/tuple keyword? any?)))
 (s/def :nuzzle/user-config
   (s/and
    (s/keys :req [:nuzzle/base-url])
    (s/every :nuzzle/config-entry)))
 (s/explain
  :nuzzle/user-config
  {:nuzzle/base-url "https://test.com"
   [:blog-posts :test-post] {:nuzzle/title "hi" :nuzzle/rss? true :nuzzle/modified "bad-datetime"}}))
