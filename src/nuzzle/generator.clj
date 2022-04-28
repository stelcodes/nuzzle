(ns nuzzle.generator
  (:require [clojure.pprint :refer [pprint]]
            [nuzzle.hiccup :as hiccup]
            [nuzzle.log :as log]
            [nuzzle.markdown :as md]
            [nuzzle.util :as util]))

(defn create-tag-index
  "Create a map of pages that are the tag index pages"
  [site-data]
  (->> site-data
       ;; Create a map shaped like tag -> [page-ids]
       (reduce-kv
        (fn [m id {:keys [tags]}]
          ;; merge-with is awesome!
          (if tags (merge-with into m (zipmap tags (repeat #{id}))) m))
        {})
       ;; Then change the val into a map with more info
       (reduce-kv
        (fn [m tag ids]
          (assoc m [:tags tag] {:index ids
                                :title (str "#" (name tag))}))
        {})))

(defn create-group-index
  "Create a map of all pages that serve as a location-based index for other
  pages. For example, if there is an entry in site-data with key
  [:blog-posts :foo], then this function will create a map with a [:blog-posts]
  entry and the value will be a map with :index [[:blog-posts :foo]]."
  [site-data]
  (->> site-data
       ;; Create a map shaped like group -> [page-ids]
       (reduce-kv
        (fn [index-map id _]
          (loop [trans-index-map index-map
                 trans-id id]
            (if-not (and (vector? trans-id) (> (count trans-id) 0))
              trans-index-map
              (let [parent-id (vec (butlast trans-id))]
                (recur (update trans-index-map parent-id
                               #(if % (conj % trans-id) #{trans-id}))
                       parent-id)))))
        {})
       ;; Then change the val into a map with more info
       (reduce-kv
        (fn [m group-id ids]
          (if-let [title (last group-id)]
            (assoc m group-id {:index ids
                               :title (util/kebab-case->title-case title)})
            (assoc m group-id {:index ids})))
        {})))

(defn realize-webpages
  "Adds :uri, :render-markdown keys to each page in the site-data."
  [{:keys [site-data] :as config}]
  {:pre [(map? site-data)]}
  (->> site-data
       (reduce-kv
        (fn [m id {:keys [markdown uri] :as v}]
          (if (vector? id)
            (assoc m id (merge v {:uri (or uri (util/id->uri id))
                                  :render-markdown
                                  (md/create-render-markdown-fn id markdown config)}))
            (assoc m id (merge v {:render-markdown
                                  (md/create-render-markdown-fn id markdown config)}))))
        {})
       (assoc config :site-data)))

(defn gen-get-site-data
  "Generate the helper function get-site-data from the realized-site-data. This
  function takes a page id (vector of 0 or more keywords) and returns the page
  information with added key :get-site-data with value get-site-data function attached."
  [realized-site-data]
  {:pre [(map? realized-site-data)] :post [(fn? %)]}
  (fn get-site-data
    ([] (->> realized-site-data
             util/convert-site-data-to-vector
             (map #(assoc % :get-site-data get-site-data))))
    ([id]
     (if-let [entity (get realized-site-data id)]
       (assoc entity :get-site-data get-site-data)
       (throw (ex-info (str "get-site-data error: id " id " not found")
                       {:id id}))))))

(defn realize-site-data
  "Creates fully realized site-data datastructure with or without drafts."
  [{:keys [remove-drafts? site-data] :as config}]
  {:pre [(vector? site-data)] :post [#(map? %)]}
  ;; Allow users to define their own overrides via deep-merge
  (-> config
      (update :site-data #(if-not remove-drafts? %
                            (do (log/log-remove-drafts)
                              (remove :draft? %))))
      (update :site-data #(util/convert-site-data-to-map %))
      (update :site-data #(util/deep-merge % (create-tag-index %)))
      (update :site-data #(util/deep-merge % (create-group-index %)))
      (realize-webpages)))

(defn generate-webpage-list
  "Creates a seq of maps which each represent a page in the website."
  [{:keys [site-data] :as _config}]
  {:pre [(map? site-data)] :post [(seq? %)]}
  (->> site-data
       ;; If key is vector, then it is a page
       (reduce-kv (fn [page-list id v]
                    (if (vector? id)
                      ;; Add the page id to the map
                      (conj page-list (assoc v :id id))
                      page-list)) [])
       ;; Add get-site-data helper function to each page
       (map #(assoc % :get-site-data (gen-get-site-data site-data)))))

(defn generate-site-index
  "Creates a map where the keys are relative URIs and the values are maps
  representing the web page. This datastructure is for the Stasis library."
  [{:keys [render-webpage] :as config} debug?]
  {:pre [(fn? render-webpage)] :post [(map? %)]}
  (->> config
       generate-webpage-list
       (map (fn [page] (when-let [render-result (render-webpage page)]
                         [(:uri page)
                          (fn [_]
                            (when debug?
                              (log/info "⚡🐈 Rendering page:\n"
                                        (with-out-str (pprint page))))
                            (str "<!DOCTYPE html>"
                                 (hiccup/html render-result)))])))
       (into {})))


