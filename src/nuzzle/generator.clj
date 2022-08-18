(ns nuzzle.generator
  (:require
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]))

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
