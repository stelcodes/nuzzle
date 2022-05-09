(ns nuzzle.schemas)

(def id [:or [:vector keyword?] keyword?])

(def site-data
  [:set {:min 1}
   [:and
    [:map
     [:id id]
     [:tags {:optional true}
      [:set keyword?]]
     [:index {:optional true}
      [:set id]]]
    [:fn {:error/message ":site-data map with {:rss? true} needs a :title or :description"}
     (fn [{:keys [rss? title description]}]
       (or (not rss?) (or title description)))]]])

(def markdown-opts
  [:map
   {:optional true}
   [:syntax-highlighting
    {:optional true}
    [:map
     ;; TODO: Add option to specify custom highlighting command
     [:provider [:enum :chroma :pygments]]
     [:style {:optional true} [:or :string :nil]]
     [:line-numbers? {:optional true} :boolean]]]
   [:shortcode-fns
    {:optional true}
    [:map-of :keyword :symbol]]])

(def config
  [:map
   {:closed true}
   [:site-data site-data]
   [:render-webpage fn?]
   [:markdown-opts {:optional true} markdown-opts]
   [:overlay-dir {:optional true} string?]
   [:export-dir {:optional true} string?]
   [:rss-channel {:optional true} [:map {:closed true}
                                   [:title string?]
                                   [:link string?]
                                   [:description string?]]]
   [:remove-drafts? {:optional true} boolean?]
   [:server-port {:optional true} [:and int? [:> 1023] [:< 65536]]]])
