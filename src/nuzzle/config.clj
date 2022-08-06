(ns nuzzle.config
  (:require
   [clojure.edn :as edn]
   [clojure.spec.alpha :as s]
   [nuzzle.log :as log]
   [nuzzle.schemas]
   [nuzzle.generator :as gen]))

(defn validate-config [config]
  (if (s/valid? :nuzzle/user-config config)
    config
    (let [errors (s/explain-str :nuzzle/user-config config)]
      (log/error "Encountered error in nuzzle.edn config:")
      (print errors)
      (throw (ex-info "Invalid Nuzzle config"
                      (s/explain-data :nuzzle/user-config config))))))

(defn read-config-path
  "Read the config from EDN file"
  [config-path]
  {:pre [(string? config-path)]}
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
      (throw e))))

(defn transform-config
  "Applies defaults and transformations to a valid :nuzzle/user-config"
  [{:nuzzle/keys [render-page] :as config} & {:as config-overrides}]
  {:pre [(map? config) (or (nil? config-overrides) (map? config-overrides))]
   :post [(map? %)]}
  (let [config-defaults {:nuzzle/publish-dir "out" :nuzzle/server-port 6899}
        resolve-sym (fn resolve-sym [config-key sym]
                      (try (var-get (requiring-resolve sym))
                        (catch Exception e
                          (log/error config-key "symbol" sym "cannot be resolved")
                          (throw e))))
        render-page-fn (resolve-sym :nuzzle/render-page render-page)
        str->time #(java.time.LocalDate/parse %)
        config (-> (merge config-defaults config config-overrides)
                   (assoc :nuzzle/render-page render-page-fn))]
    (reduce-kv
     (fn [acc k v]
       (if-not (and (vector? k) (map? v))
         (assoc acc k v)
         (assoc acc k
                (cond-> v
                  (:modified v) (update :modified str->time)))))
     {} config)))

(defn load-specified-config
  "Read the site-data EDN file and validate it."
  [config-path config-overrides]
  (-> config-path
      read-config-path
      validate-config
      (transform-config config-overrides)
      (gen/realize-site-data)))

(defn load-default-config [config-overrides]
  (load-specified-config "nuzzle.edn" config-overrides))

(comment (load-specified-config "test-resources/config-1.edn" {}))
