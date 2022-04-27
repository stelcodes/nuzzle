(ns nuzzle.config
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [malli.core :as m]
            [malli.error :as me]
            [nuzzle.log :as log]))

(def site-data-spec
  [:and
   [:vector {:min 1}
    [:and
     [:map [:id [:or [:vector keyword?] keyword?]]]
     [:fn {:error/message ":site-data map with {:rss? true} needs a :title or :description"}
      (fn [{:keys [rss? title description]}]
        (or (not rss?) (or title description)))]]]
   [:fn {:error/message "missing homepage :id []"}
    (fn [x] (some #(= [] (:id %)) x))]])

(def config-spec
   [:map
    {:closed true}
    [:site-data site-data-spec]
    [:render-webpage fn?]
    [:overlay-dir {:optional true} string?]
    [:output-dir {:optional true} string?]
    [:highlight-style {:optional true} string?]
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
    (do (log/error "Encountered errors in nuzzle.edn config:")
      (->> config
        (m/explain config-spec)
        (me/humanize)
        pp/pprint)
      (throw (ex-info "Invalid Nuzzle config" {})))))

(defn load-specified-config
  "Read the site-data EDN file and validate it."
  [config-path config-overrides]
  {:pre [(string? config-path) (or (nil? config-overrides) (map? config-overrides))]
   :post [(map? %)]}
  (let [config-defaults {:output-dir "out" :dev-port 6899}
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
        (assoc :render-webpage render-webpage-fn)
        (validate-config))))

(defn load-config [config-overrides]
  (load-specified-config "nuzzle.edn" config-overrides))

(comment (load-specified-config "test-resources/config-1.edn" {}))
