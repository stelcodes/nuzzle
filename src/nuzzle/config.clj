(ns nuzzle.config
  (:require
   [clojure.edn :as edn]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [nuzzle.content :as content]
   [nuzzle.log :as log]
   [nuzzle.schemas :as schemas]
   [nuzzle.generator :as gen]
   [nuzzle.util :as util]
   ;; Register spell-spec expound helpers after requiring expound.alpha
   [spell-spec.expound]))

(defn validate-config [config]
  ;; Redefine valid authors
  (schemas/redefine-page-author-spec config)
  (schemas/redefine-feed-author-spec config)
  (if (s/valid? :nuzzle/user-config config)
    config
    (do (expound/expound :nuzzle/user-config config {:theme :figwheel-theme})
      (log/error "Encountered error in Nuzzle config:")
      (throw (ex-info (str "Invalid Nuzzle config! "
                           (re-find #"failed: .*" (s/explain-str :nuzzle/user-config config)))
                      {})))))

(defn read-config-from-path
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

(defn read-default-config []
  (read-config-from-path "nuzzle.edn"))

(defn transform-config
  "Creates fully transformed config with or without drafts."
  [{:nuzzle/keys [build-drafts?] :as config}]
  {:pre [(map? config)] :post [#(map? %)]}
  ;; Allow users to define their own overrides via deep-merge
  (letfn [(apply-defaults [config]
            (let [config-defaults {:nuzzle/publish-dir "out"
                                   :nuzzle/server-port 6899}]
              (merge config-defaults config)))
          (handle-drafts [config]
            (if build-drafts?
              (do (log/log-build-drafts) config)
              (do (log/log-remove-drafts)
                (reduce-kv
                 (fn [acc k v]
                   (if (and (vector? k) (:nuzzle/draft? v))
                     acc
                     (assoc acc k v)))
                 {} config))))
          (symbol->value [sym] (var-get (requiring-resolve sym)))
          (resolve-symbols [config]
            (update config :nuzzle/render-page symbol->value))
          (convert-time-strs [config]
            (reduce-kv
             (fn [acc ckey {:nuzzle/keys [updated] :as cval}]
               (if-not (and (vector? ckey) (map? cval))
                 (assoc acc ckey cval)
                 (assoc acc ckey
                        (cond-> cval
                          updated (update :nuzzle/updated util/time-str->?inst)))))
             {} config))
          (add-page-keys [config]
            (reduce-kv
             (fn [acc ckey {:nuzzle/keys [content] :as cval}]
               (assoc acc ckey
                      (cond-> cval
                        (vector? ckey) (assoc :nuzzle/url (util/page-key->url ckey)
                                              :nuzzle/page-key ckey)
                        (or (vector? ckey) content) (assoc :nuzzle/render-content
                                                           (content/create-render-content-fn ckey config)))))
             {} config))
          (add-get-config-to-pages [config]
            (let [get-config (gen/gen-get-config config)]
              (reduce-kv
               (fn [acc ckey cval]
                 (assoc acc ckey (cond-> cval
                                   (vector? ckey) (assoc :nuzzle/get-config get-config))))
               {} config)))]
    (as-> config $
      (apply-defaults $)
      (handle-drafts $)
      (resolve-symbols $)
      (convert-time-strs $)
      (util/deep-merge $ (gen/create-tag-index-page-entries $))
      (util/deep-merge $ (gen/create-hierarchical-index-page-entries $))
      (add-page-keys $)
      ;; Adding get-config must come after all other transformations
      (add-get-config-to-pages $))))

(defn load-config-from-path
  "Read a config EDN file and validate it."
  [config-path & {:as config-overrides}]
  (-> config-path
      read-config-from-path
      (util/deep-merge config-overrides)
      validate-config
      transform-config))

(defn load-default-config [& {:as config-overrides}]
  (load-config-from-path "nuzzle.edn" config-overrides))

(comment (load-config-from-path "test-resources/edn/config-1.edn" {}))
