(ns codes.stel.nuzzle.generator
  (:require [babashka.fs :as fs]
            [clj-rss.core :as rss]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [codes.stel.nuzzle.hiccup :as hiccup]
            [codes.stel.nuzzle.log :as log]
            [codes.stel.nuzzle.util :as util]
            [markdown.core :refer [md-to-html-string]]
            [stasis.core :as stasis]))

(defn load-site-config
  "Turn the site-config into a map. It should be a path to an edn file. Load
  that file make sure it as a map."
  [site-config]
  {:pre [(string? site-config)] :post [(map? %)]}
  (try
    (-> site-config
        (io/file)
        (slurp)
        (edn/read-string))
    (catch Throwable _
      (throw (ex-info
              (str "Site config file: " site-config " could not be read. Make sure the file exists and the contents are a valid EDN map.")
              {:config site-config})))))

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

(defn create-render-content-fn
  "Create a function that turned the :content file into html, wrapped with the
  hiccup raw identifier."
  [id content]
  {:pre [(vector? id) (or (nil? content) (string? content))]}
  (if-not content
    ;; If :content is not defined, just make a function that returns nil
    (constantly nil)
    (if-let [content-file (io/resource content)]
      (let [ext (fs/extension content-file)]
        (cond
         ;; If a html or svg file, just slurp it up
         (or (= "html" ext) (= "svg" ext))
         (fn render-html []
           (hiccup/raw (string/trim (slurp content-file))))
         ;; If markdown, convert to html
         (or (= "markdown" ext) (= "md" ext))
         (fn render-markdown []
           (hiccup/raw (md-to-html-string (slurp content-file))))
         ;; If extension not recognized, throw Exception
         :else (throw (ex-info (str "Filetype of content file " content " for id " id " not recognized")
                      {:id id :content content}))))
      ;; If content-file is defined but it can't be found, throw an Exception
      (throw (ex-info (str "Resource " content " for id " id " not found")
                      {:id id :content content})))))

(defn realize-pages
  "Adds :uri, :render-content keys to each page in the site-config."
  [site-config]
  {:pre [map? site-config]}
  (reduce-kv
   (fn [m id {:keys [content uri] :as v}]
     (if (vector? id)
       (assoc m id
              (merge v {:uri (or uri (util/id->uri id))
                        :render-content
                        (create-render-content-fn id content)}))
       (assoc m id v)))
   {} site-config))

(defn gen-id->info
  "Generate the helper function id->info from the realized-site-config. This
  function takes a page id (vector of 0 or more keywords) and returns the page
  information with added key :id->info with value id->info function attached."
  [realized-site-config]
  {:pre [(map? realized-site-config)] :post [(fn? %)]}
  (fn id->info [id]
    (if-let [entity (get realized-site-config id)]
      (assoc entity :id->info id->info)
      (throw (ex-info (str "id->info error: id " id " not found")
                      {:id id})))))

(defn remove-drafts
  "Remove page entries from site-config map if they are marked as a draft with
  :draft? true kv pair."
  [site-config]
  (reduce-kv
   (fn [m id {:keys [draft?] :as v}]
     (if (and (vector? id) draft?)
       m
       (assoc m id v)))
   {}
   site-config))

(defn realize-site-config
  "Creates fully realized site-config datastructure with or without drafts."
  [site-config remove-drafts?]
  {:pre [(map? site-config) (boolean? remove-drafts?)]}
  ;; Allow users to define their own overrides via deep-merge
  (let [site-config (if remove-drafts?
                      (remove-drafts site-config)
                      site-config)]
    (->> site-config
         ;; Make sure there is a root index.html file
         (util/deep-merge {[] {:uri "/"}})
         (util/deep-merge (create-group-index site-config))
         (util/deep-merge (create-tag-index site-config))
         (realize-pages))))

(defn generate-page-list
  "Creates a seq of maps which each represent a page in the website."
  [realized-site-config]
  {:pre [(map? realized-site-config)] :post [(seq? %)]}
  (->> realized-site-config
       ;; If key is vector, then it is a page
       (reduce-kv (fn [page-list id v]
                    (if (vector? id)
                      ;; Add the page id to the map
                      (conj page-list (assoc v :id id))
                      page-list)) [])
       ;; Add id->info helper function to each page
       (map #(assoc % :id->info (gen-id->info realized-site-config)))))

(defn generate-site-index
  "Creates a map where the keys are relative URIs and the values are maps
  representing the web page. This datastructure is for the Stasis library."
  [page-list render-page debug?]
  {:pre [(seq? page-list) (fn? render-page)] :post [(map? %)]}
  (->> page-list
       (map (fn [page] (when-let [render-result (render-page page)]
                         [(:uri page)
                          (fn [_]
                            (when debug?
                              (log/info "⚡🐈 Rendering page:\n"
                                        (with-out-str (pprint page))))
                            (str "<!DOCTYPE html>"
                                 (hiccup/html render-result)))])))
       (into {})))

(defn create-rss-feed
  "Creates a string of XML that is a valid RSS feed"
  ;; TODO: make sure that clj-rss baked in PermaLink=false is ok
  [realized-site-config {:keys [author link] :as rss-opts}]
  {:pre [(map? realized-site-config) (map? rss-opts) (string? author)]
   :post [(string? %)]}
  (when rss-opts
    (apply rss/channel-xml
           (select-keys rss-opts [:title :description :link])
           (->>
            (for [{:keys [uri title rss]} (vals realized-site-config)]
              (when rss
                (-> {:title (or title "Untitled") :guid (str link uri) :author author}
                    (merge (when (map? rss) rss))
                    util/remove-nil-values)))
            (remove nil?)))))

(defn export-site-index
  [site-index static-dir target-dir]
  (when (and static-dir (not (io/resource static-dir)))
    (throw (ex-info (str static-dir " is not a valid resource directory")
                    {:static-dir static-dir})))
  (let [assets (when static-dir (io/resource static-dir))]
    (fs/create-dirs target-dir)
    (stasis/empty-directory! target-dir)
    (stasis/export-pages site-index target-dir)
    (when assets (fs/copy-tree assets target-dir))))

