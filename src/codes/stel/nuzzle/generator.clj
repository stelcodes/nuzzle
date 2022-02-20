(ns codes.stel.nuzzle.generator
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [codes.stel.nuzzle.hiccup :as hiccup]
            [codes.stel.nuzzle.util :as util]
            [markdown.core :refer [md-to-html-string]]
            [stasis.core :as stasis]
            [taoensso.timbre :as log]))

(defn load-site-config
  "Turn the site-config into a map. It can be defined as a map or a string. If
  it is a string, it should be a path to an edn resource. Attempt to load that
  resource and make sure it as a map."
  [site-config]
  {:pre [(or (map? site-config) (string? site-config))] :post [(map? %)]}
  (if (map? site-config)
    site-config
    (try
      (-> site-config
          (io/resource)
          (slurp)
          (edn/read-string))
      (catch Throwable _
        (throw (ex-info
                (str "Site config file: " site-config " could not be read. Make sure the file is in your classpath and the contents are a valid EDN map.")
                {:config site-config}))))))

(defn create-tag-index
  "Create a map of pages that are the tag index pages"
  [site-config]
  (->> site-config
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
  pages. For example, if there is an entry in site-config with key
  [:blog-posts :foo], then this function will create a map with a [:blog-posts]
  entry and the value will be a map with :index [[:blog-posts :foo]]."
  [config]
  (->> config
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

(defn create-render-resource-fn
  "Create a function that turned the resource into html, wrapped with the
  hiccup raw identifier."
  [id resource]
  {:pre [(vector? id) (or (nil? resource) (string? resource))]}
  (if-not resource
    ;; If resource is not defined, just make a function that returns nil
    (constantly nil)
    (if-let [resource-file (io/resource resource)]
      (let [ext (fs/extension resource-file)]
        (cond
         ;; If a html or svg file, just slurp it up
         (or (= "html" ext) (= "svg" ext))
         (fn render-html []
           (hiccup/raw (slurp resource-file)))
         ;; If markdown, convert to html
         (or (= "markdown" ext) (= "md" ext))
         (fn render-markdown []
           (hiccup/raw (md-to-html-string (slurp resource-file))))
         ;; If extension not recognized, throw Exception
         :else (throw (ex-info (str "Resource " resource " filetype for id " id " not recognized")
                      {:id id :resource resource}))))
      ;; If a resource file is defined but it can't be found, throw an Exception
      (throw (ex-info (str "Resource " resource " for id " id " not found")
                      {:id id :resource resource})))))
