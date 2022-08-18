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
