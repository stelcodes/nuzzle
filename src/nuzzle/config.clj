(ns nuzzle.config
  (:require
   [clojure.edn :as edn]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [nuzzle.content :as content]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.schemas :as schemas]
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

(defn create-tag-index-page-entries
  "Add config page entries for pages that index all the pages which are tagged
  with a particular tag. Each one of these tag index pages goes under the
  /tags/ subdirectory"
  [config]
  (->> config
       ;; Create a map shaped like {tag-kw #{page-keys-with-tag}}
       (reduce-kv
        ;; For every key value pair in config
        (fn [acc ckey {:nuzzle/keys [tags]}]
          (if (and (vector? ckey) tags)
            ;; If entry is a page with tags, create a map with an entry for
            ;; every tag the page is tagged with and merge it into acc
            (merge-with into acc (zipmap tags (repeat #{ckey})))
            ;; if entry is an option or tagless page, don't change acc
            acc))
        {})
       ;; Then change each entry into a proper page entry
       (reduce-kv
        (fn [acc tag ckeys]
          (assoc acc [:tags tag] {:nuzzle/index ckeys
                                  :nuzzle/title (str "#" (name tag))}))
        {})))

(defn create-hierarchical-index-page-entries
  "Create config page entries for pages that are hierarchically located 'above'
  the page entries present in the config according to their path such that all
  pages have parent pages going up to the root page"
  [config]
  (->> config
       ;; Create a map shaped like {page-key #{child-page-keys}}
       (reduce-kv
        ;; For every key value pair in config
        (fn [acc ckey _]
          ;; Start loop because each config entry may add multiple entries to acc
          (loop [acc acc
                 ckey ckey]
            (if (or (not (vector? ckey)) (empty? ckey))
              ;; If config key is option or root page, don't change acc map
              acc
              ;; If config key is a non-root page, associate with parent page key in the acc map
              (let [parent-ckey (vec (butlast ckey))]
                ;; Add the page key to acc map in a set under the parent key
                (recur (update acc parent-ckey #(if % (conj % ckey) #{ckey}))
                       ;; Repeat the process with parent key
                       parent-ckey)))))
        {})
       ;; Then change the vals into proper page maps
       (reduce-kv
        (fn [acc ckey child-pages]
          (let [title-kw (last ckey)
                title (if title-kw (util/kebab-case->title-case title-kw) "Home")]
            (assoc acc ckey {:nuzzle/index child-pages :nuzzle/title title})))
        {})))

(defn create-get-config
  "Create the helper function get-config from the transformed config. This
  function takes a config key and returns the corresponding value with added
  key :nuzzle/get-config with value get-config function attached."
  [config]
  {:pre [(map? config)] :post [(fn? %)]}
  (fn get-config
    ([& ckeys]
     ;; If no args, return the whole config
     (if (empty? ckeys)
       (reduce-kv (fn [acc ckey cval]
                    (assoc acc ckey
                           (cond-> cval
                             ;; Add get-config to all page entry maps
                             (vector? ckey) (assoc :nuzzle/get-config get-config))))
                  {} config)
       (reduce (fn [last-match ckey]
                 (if (try (contains? last-match ckey) (catch Throwable _ false))
                   (let [next-match (get last-match ckey)]
                     (cond-> next-match
                       ;; Only add get-config to returned value if it's a page entry map
                       (and (vector? ckey) (map? next-match) (:nuzzle/title next-match))
                       (assoc :nuzzle/get-config get-config)))
                   (throw (ex-info (str "get-config error: config key sequence "
                                        (-> ckeys vec pr-str) " cannot be resolved")
                                   {:invalid-key ckey}))))
               config ckeys)))))

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
            (let [get-config (create-get-config config)]
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
      (util/deep-merge $ (create-tag-index-page-entries $))
      (util/deep-merge $ (create-hierarchical-index-page-entries $))
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

(defn create-site-index
  "Creates a map where the keys are relative URLs and the values are a string
  of HTML or a function that produces a string of HTML. This datastructure is
  defined by stasis."
  [{:nuzzle/keys [render-page] :as config} & {:keys [lazy?]}]
  {:pre [(fn? render-page)] :post [(map? %)]}
  (reduce-kv
   (fn [acc ckey cval]
     (if-let [render-result (and (vector? ckey) (render-page cval))]
       (assoc acc (:nuzzle/url cval)
              (if lazy?
                ;; Turn the page's hiccup into HTML on the fly
                (fn [_]
                  (log/log-rendering-page cval)
                  (hiccup/html-document render-result))
                (hiccup/html-document render-result)))
       ;; If not a page entry, skip it
       acc))
   {} config))
