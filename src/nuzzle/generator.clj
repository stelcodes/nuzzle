(ns nuzzle.generator
  (:require [babashka.fs :as fs]
            [clojure.pprint :refer [pprint]]
            [nuzzle.hiccup :as hiccup]
            [nuzzle.log :as log]
            [nuzzle.markdown :as md]
            [nuzzle.util :as util]
            [stasis.core :as stasis]))

(defn create-tag-index
  "Create a map of pages that are the tag index pages"
  [site-data]
  (->> site-data
       ;; Create a map shaped like tag -> [page-ids]
       (reduce-kv
        (fn [m id {:keys [tags]}]
          ;; merge-with is awesome!
          (if tags (merge-with into m (zipmap tags (repeat [id]))) m))
        {})
       ;; Then change the val into a map with more info
       (reduce-kv
        (fn [m tag ids]
          (assoc m [:tags tag] {:index ids
                                :title (str "#" (name tag))
                                :uri (str "/tags/" (name tag) "/")}))
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
        (fn [m id _]
          (if (and (vector? id) (> (count id) 1))
            (merge-with into m {(vec (butlast id)) [id]}) m))
        {})
       ;; Then change the val into a map with more info
       (reduce-kv
        (fn [m group-id ids]
          (assoc m group-id {:index ids
                             :title (util/kebab-case->title-case (last group-id))
                             :uri (util/id->uri group-id)}))
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

(defn generate-page-list
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
       generate-page-list
       (map (fn [page] (when-let [render-result (render-webpage page)]
                         [(:uri page)
                          (fn [_]
                            (when debug?
                              (log/info "⚡🐈 Rendering page:\n"
                                        (with-out-str (pprint page))))
                            (str "<!DOCTYPE html>"
                                 (hiccup/html render-result)))])))
       (into {})))

(defn export-site
  [{:keys [overlay-dir export-dir] :as config}]
  (fs/create-dirs export-dir)
  (stasis/empty-directory! export-dir)
  (-> config
      (generate-site-index false)
      (stasis/export-pages export-dir))
  (when overlay-dir
    (log/log-overlay-dir overlay-dir)
    (util/ensure-overlay-dir overlay-dir)
    (fs/copy-tree overlay-dir export-dir)))

