(ns nuzzle.config
  (:require [clojure.edn :as edn]
            [nuzzle.log :as log]))

(defn validate-config [{:keys [site-data] :as config}]
  (let [missing-homepage? (not (some #(= [] (:id %)) site-data))]
    (cond
     missing-homepage?
     (throw (ex-info "Site data is missing homepage (webpage map with an :id of [])" {}))
     :else config)))

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
