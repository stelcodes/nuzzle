(ns nuzzle.generator
  (:require
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
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

(defn create-hierarchical-index-page-entries
  "Create config page entries for pages that are hierarchically located 'above'
  the page entries present in the config according to their path such that all
  pages have parent pages going up to the root page"
  [config]
  (->> config
       ;; Create a map shaped like {page-key #{child-page-keys}}
       (reduce-kv
        ;; for every key value pair in config
        (fn [acc ckey _]
          ;; start loop because each config entry may add multiple entries to acc
          (loop [acc acc
                 ckey ckey]
            (if (or (not (vector? ckey)) (empty? ckey))
              ;; if config key is option or root page, don't change acc map
              acc
              ;; if config key is a non-root page, associate with parent page key in the acc map
              (let [parent-ckey (vec (butlast ckey))]
                ;; add the page key to acc map in a set under the parent key
                (recur (update acc parent-ckey #(if % (conj % ckey) #{ckey}))
                       ;; repeat the process with parent key
                       parent-ckey)))))
        {})
       ;; Then change the vals into proper page maps
       (reduce-kv
        (fn [acc ckey child-pages]
          (let [title-kw (last ckey)
                title (if title-kw (util/kebab-case->title-case title-kw) "Home")]
            (assoc acc ckey {:nuzzle/index child-pages :nuzzle/title title})))
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

(defn generate-page-list
  "Creates a seq of maps which each represent a page in the website."
  [config]
  {:pre [(map? config)] :post [(seq? %)]}
       (reduce-kv (fn [acc ckey cval]
                    (if (vector? ckey) (conj acc cval) acc))
                  (list) config))

(defn generate-debug-site-index
  "Creates a map where the keys are URLs and the values are functions that log
  the page map and return the page's Hiccup. This datastructure is
  defined by stasis."
  [{:nuzzle/keys [render-page] :as config}]
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
  [{:nuzzle/keys [render-page] :as config}]
  {:pre [(fn? render-page)] :post [(map? %)]}
  (->> config
       generate-page-list
       (map (fn [page] (when-let [render-result (render-page page)]
                         [(:nuzzle/url page)
                          (hiccup/html-document render-result)])))
       (into {})))
