(ns nuzzle.schemas)

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

(def markdown-opts
  [:map
   {:optional true
    :closed true}])

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
   [:markdown-opts {:optional true} markdown-opts]
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
