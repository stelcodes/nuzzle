(ns nuzzle.generator
  (:require
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.content :as con]
   [nuzzle.util :as util]))

(defn create-tag-index
  "Create a map of pages that are the tag index pages"
  [config]
  (->> config
       ;; Create a map shaped like tag -> [page-keys]
       (reduce-kv
        (fn [acc pkey {:nuzzle/keys [tags]}]
          ;; merge-with is awesome!
          (if (and (vector? pkey) tags) (merge-with into acc (zipmap tags (repeat #{pkey}))) acc))
        {})
       ;; Then change the val into a map with more info
       (reduce-kv
        (fn [acc tag pkeys]
          (assoc acc [:tags tag] {:nuzzle/index pkeys
                                  :nuzzle/title (str "#" (name tag))}))
        {})))

(defn create-group-index
  "Create a map of all pages that serve as a location-based index for other
  pages"
  [config]
  (->> config
       ;; Create a map shaped like group -> [page-keys]
       (reduce-kv
        (fn [acc pkey _]
          (loop [trans-acc acc
                 trans-pkey pkey]
            (if-not (and (vector? trans-pkey) (> (count trans-pkey) 0))
              trans-acc
              (let [parent-pkey (vec (butlast trans-pkey))]
                (recur (update trans-acc parent-pkey
                               #(if % (conj % trans-pkey) #{trans-pkey}))
                       parent-pkey)))))
        {})
       ;; Then change the val into a page map with more info
       (reduce-kv
        (fn [m parent-pkey children-pkeys]
          (if-let [title (last parent-pkey)]
            (assoc m parent-pkey {:nuzzle/index children-pkeys
                                  :nuzzle/title (util/kebab-case->title-case title)})
            (assoc m parent-pkey {:nuzzle/index children-pkeys
                                  :nuzzle/title "Home"})))
        {})))

(defn gen-get-config
  "Generate the helper function get-config from the transformed config. This
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
       (reduce (fn [acc ckey]
                 (if (try (contains? acc ckey) (catch Throwable _ false))
                   (cond-> (get acc ckey)
                     ;; Only add get-config to returned value if it's a page entry map
                     (and (vector? ckey) (map? acc) (:nuzzle/title acc))
                     (assoc :nuzzle/get-config get-config))
                   (throw (ex-info (str "get-config error: config key sequence "
                                        (-> ckeys vec pr-str) " cannot be resolved")
                                   {:invalid-key ckey}))))
               config ckeys)))))

(defn transform-config
  "Creates fully transformed config with or without drafts."
  [{:nuzzle/keys [build-drafts?] :as config}]
  {:pre [(map? config)] :post [#(map? %)]}
  ;; Allow users to define their own overrides via deep-merge
  (letfn [(handle-drafts [config]
            (if build-drafts?
              (do (log/log-build-drafts) config)
              (do (log/log-remove-drafts)
                (reduce-kv
                 (fn [acc k v]
                   (if (and (vector? k) (:nuzzle/draft? v))
                     acc
                     (assoc acc k v)))
                 {} config))))
          (transform-pages [config]
            (reduce-kv
             (fn [acc ckey {:nuzzle/keys [content] :as cval}]
               (assoc acc ckey
                      (cond-> cval
                        (vector? ckey) (assoc :nuzzle/url (util/page-key->url ckey))
                        (or (vector? ckey) content) (assoc :nuzzle/render-content
                                                           (con/create-render-content-fn ckey config)))))
             {} config))]
    (as-> config $
      (handle-drafts $)
      (util/deep-merge $ (create-tag-index $))
      (util/deep-merge $ (create-group-index $))
      (transform-pages $))))

(defn generate-page-list
  "Creates a seq of maps which each represent a page in the website."
  [config]
  {:pre [(map? config)] :post [(seq? %)]}
  (->> config
       ;; If key is vector, then it is a page
       (reduce-kv (fn [acc ckey cval]
                    (if (vector? ckey)
                      ;; Add the page key to the map
                      (conj acc (assoc cval :nuzzle/page-key ckey))
                      acc))
                  [])
       ;; Add get-config helper function to each page
       (map #(assoc % :nuzzle/get-config (gen-get-config config)))))

(defn generate-debug-site-index
  "Creates a map where the keys are URLs and the values are functions that log
  the page map and return the page's Hiccup. This datastructure is
  defined by stasis."
  [{:keys [nuzzle/render-page] :as config}]
  {:pre [(fn? render-page)] :post [(map? %)]}
  (->> config
       generate-page-list
       (map (fn [page] (when-let [render-result (render-page page)]
                         [(:nuzzle/url page)
                          (fn [_]
                            (log/log-rendering-page page)
                            (hiccup/html-document render-result))])))
       (into {})))

(defn generate-rendered-site-index
  "Creates a map where the keys are relative URLs and the values are Hiccup.
  This datastructure is defined by stasis."
  [{:keys [nuzzle/render-page] :as config}]
  {:pre [(fn? render-page)] :post [(map? %)]}
  (->> config
       generate-page-list
       (map (fn [page] (when-let [render-result (render-page page)]
                         [(:nuzzle/url page)
                          (hiccup/html-document render-result)])))
       (into {})))
