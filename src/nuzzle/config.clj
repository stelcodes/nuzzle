(ns nuzzle.config
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [malli.core :as m]
            [malli.error :as me]
            [nuzzle.log :as log]
            [nuzzle.generator :as gen]))

(def id-spec [:or [:vector keyword?] keyword?])

(def site-data-spec
  [:set {:min 1}
   [:and
    [:map
     [:id id-spec]
     [:tags {:optional true}
      [:set keyword?]]
     [:index {:optional true}
      [:set id-spec]]]
    [:fn {:error/message ":site-data map with {:rss? true} needs a :title or :description"}
     (fn [{:keys [rss? title description]}]
       (or (not rss?) (or title description)))]]])

(def markdown-spec
  [:map
   {:optional true}
   [:syntax-highlighting
    {:optional true}
    [:map
     ;; TODO: Add option to specify custom highlighting command
     [:provider [:enum :chroma :pygment]]
     ;; TODO: Validate style, turn into enum keyword, print out valid styles on schema error
     [:style {:optional true} [:or :string :nil]]]]
   [:shortcode-fns
    {:optional true}
    [:map-of :keyword :symbol]]])

(def config-spec
  [:map
   {:closed true}
   [:site-data site-data-spec]
   [:render-webpage fn?]
   [:markdown {:optional true} markdown-spec]
   [:overlay-dir {:optional true} string?]
   [:export-dir {:optional true} string?]
   [:rss-channel {:optional true} [:map {:closed true}
                                   [:title string?]
                                   [:link string?]
                                   [:description string?]]]
   [:remove-drafts? {:optional true} boolean?]
   [:dev-port {:optional true} [:and int? [:> 1023] [:< 65536]]]])

(def valid-config?
  (m/validator config-spec))

(defn validate-config [config]
  (if (valid-config? config)
    config
      (let [errors (->> config
                        (m/explain config-spec)
                        (me/humanize))]
        (log/error "Encountered error in nuzzle.edn config:")
        (pp/pprint errors)
        (throw (ex-info "Invalid Nuzzle config" errors)))))

(defn read-specified-config
  "Read the site-data EDN file and validate it."
  [config-path config-overrides]
  {:pre [(string? config-path) (or (nil? config-overrides) (map? config-overrides))]
   :post [(map? %)]}
  (let [config-defaults {:export-dir "out" :dev-port 6899}
        edn-config
        (try
          (edn/read-string (slurp config-path))
          (catch java.io.FileNotFoundException e
            (log/error "Config file is missing or has incorrect permissions.")
            (throw e))
          (catch java.lang.RuntimeException e
            (log/error "Config file contains invalid EDN.")
            (throw e))
          (catch Exception e
            (log/error "Could not read config file.")
            (throw e)))
        {render-webpage-symbol :render-webpage :as full-config}
        (merge config-defaults edn-config config-overrides)
        render-webpage-fn
        (try (var-get (requiring-resolve render-webpage-symbol))
          (catch java.io.FileNotFoundException e
            (log/error ":render-webpage function" render-webpage-symbol "cannot be resolved")
            (throw e)))]
    (-> full-config
        (assoc :render-webpage render-webpage-fn))))

(defn load-specified-config
  "Read the site-data EDN file and validate it."
  [config-path config-overrides]
  (-> config-path
      (read-specified-config config-overrides)
      (validate-config)
      (gen/realize-site-data)))

(defn load-default-config [config-overrides]
  (load-specified-config "nuzzle.edn" config-overrides))

(comment (load-specified-config "test-resources/config-1.edn" {}))
