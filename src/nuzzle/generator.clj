(ns nuzzle.generator
  (:require [babashka.fs :as fs]
            [clj-rss.core :as rss]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [nuzzle.hiccup :as hiccup]
            [nuzzle.log :as log]
            [nuzzle.util :as util]
            [cybermonday.core :as cm]
            [stasis.core :as stasis]))

(defn convert-site-data-to-vector
  [site-data]
  {:pre [(map? site-data)] :post [#(vector? %)]}
  (->> site-data
       (reduce-kv
        (fn [agg id m]
          (conj agg (assoc m :id id)))
        [])))

(defn convert-site-data-to-map
  [site-data]
  {:pre [(vector? site-data)] :post [#(map? %)]}
  (->> site-data
       (reduce
        (fn [agg {:keys [id] :as m}]
          (assoc agg id (dissoc m :id)))
        {})))

(defn validate-config [{:keys [site-data] :as config}]
  (let [missing-homepage? (not (some #(= [] (:id %)) site-data))]
    (cond
     missing-homepage?
     (throw (ex-info "Site data is missing homepage (webpage map with an :id of [])" {}))
     :else config)))

(defn load-specified-config
  "Read the site-data EDN file and validate it."
  [config-path config-overrides]
  {:pre [(string? config-path) (or (nil? config-overrides) (map? config-overrides))]
   :post [(map? %)]}
  (let [config-defaults {:output-dir "out" :dev-port 6899}
        edn-config
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
            (throw e)))
        {render-webpage-symbol :render-webpage :as full-config}
        (merge config-defaults edn-config config-overrides)
        render-webpage-fn
        (try (var-get (requiring-resolve render-webpage-symbol))
          (catch java.io.FileNotFoundException e
            (log/error ":render-webpage function" render-webpage-symbol "cannot be resolved")
            (throw e)))]
    (-> full-config
        (assoc :render-webpage render-webpage-fn)
        (validate-config))))

(defn load-config [config-overrides]
  (load-specified-config "nuzzle.edn" config-overrides))

(comment (load-specified-config "test-resources/config-1.edn" {}))

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

(defn safe-sh [[command & _ :as args]]
  (try (apply sh args)
    (catch Exception _
      {:exit 1 :err (str "Command failed. Please ensure " command " is installed.")})))

(defn highlight-code [highlight-style language code]
  (let [code-file (fs/create-temp-file)
        code-path (str (fs/canonicalize code-file))
        _ (spit code-path code)
        chroma-command ["chroma" (str "--lexer=" language) "--formatter=html" "--html-only"
                        "--html-inline-styles" (str "--style=" highlight-style) code-path]
        {:keys [exit out err]} (safe-sh chroma-command)]
    (if (not= 0 exit)
      (do
        (log/warn "Failed to highlight code:" code-path)
        (log/warn err)
        code)
      (do
        (fs/delete-if-exists code-file)
        out))))

(defn code-block-highlighter [highlight-style [_tag-name {:keys [language]} body]]
  (if highlight-style
    [:code (hiccup/raw (highlight-code
                        highlight-style
                        (or language "no-highlight")
                        body))]
    [:code [:pre body]]))

(defn process-markdown-file [highlight-style file]
  (let [code-block-with-style (partial code-block-highlighter highlight-style)
        lower-fns {:markdown/fenced-code-block code-block-with-style
                   :markdown/indented-code-block code-block-with-style}
        [_ _ & hiccup] ; Avoid the top level :div {}
        (-> file
            slurp
            (cm/parse-body {:lower-fns lower-fns}))]
    (hiccup/html hiccup)))

(defn create-render-content-fn
  "Create a function that turned the :content file into html, wrapped with the
  hiccup raw identifier."
  [id content {:keys [highlight-style]}]
  {:pre [(vector? id) (or (nil? content) (string? content))]}
  (if-not content
    ;; If :content is not defined, just make a function that returns nil
    (constantly nil)
    (if-let [content-file (io/file content)]
      (let [ext (fs/extension content-file)]
        (cond
         ;; If a html or svg file, just slurp it up
         (or (= "html" ext) (= "svg" ext))
         (fn render-html []
           (hiccup/raw (string/trim (slurp content-file))))
         ;; If markdown, convert to html
         (or (= "markdown" ext) (= "md" ext))
         (fn render-markdown []
           (hiccup/raw (process-markdown-file highlight-style content-file)))
         ;; If extension not recognized, throw Exception
         :else (throw (ex-info (str "Filetype of content file " content " for id " id " not recognized")
                      {:id id :content content}))))
      ;; If content-file is defined but it can't be found, throw an Exception
      (throw (ex-info (str "Resource " content " for id " id " not found")
                      {:id id :content content})))))

(defn realize-pages
  "Adds :uri, :render-content keys to each page in the site-data."
  [site-data config]
  {:pre [map? site-data]}
  (reduce-kv
   (fn [m id {:keys [content uri] :as v}]
     (if (vector? id)
       (assoc m id
              (merge v {:uri (or uri (util/id->uri id))
                        :render-content
                        (create-render-content-fn id content config)}))
       (assoc m id v)))
   {} site-data))

(defn gen-get-site-data
  "Generate the helper function get-site-data from the realized-site-data. This
  function takes a page id (vector of 0 or more keywords) and returns the page
  information with added key :get-site-data with value get-site-data function attached."
  [realized-site-data]
  {:pre [(map? realized-site-data)] :post [(fn? %)]}
  (fn get-site-data
    ([] (->> realized-site-data
             convert-site-data-to-vector
             (map #(assoc % :get-site-data get-site-data))))
    ([id]
     (if-let [entity (get realized-site-data id)]
       (assoc entity :get-site-data get-site-data)
       (throw (ex-info (str "get-site-data error: id " id " not found")
                       {:id id}))))))

(defn remove-drafts
  "Remove page entries from site-data map if they are marked as a draft with
  :draft? true kv pair."
  [site-data]
  (reduce-kv
   (fn [m id {:keys [draft?] :as v}]
     (if (and (vector? id) draft?)
       m
       (assoc m id v)))
   {}
   site-data))

(defn realize-site-data
  "Creates fully realized site-data datastructure with or without drafts."
  [{:keys [remove-drafts? site-data] :as config}]
  {:pre [(vector? site-data)] :post [#(map? %)]}
  ;; Allow users to define their own overrides via deep-merge
  (let [site-data (convert-site-data-to-map site-data)
        site-data (if remove-drafts?
                    (remove-drafts site-data)
                    site-data)
        site-data (->> site-data
                       ;; Make sure there is a root index.html file
                       ;; (util/deep-merge {[] {:uri "/"}})
                       (util/deep-merge (create-group-index site-data))
                       (util/deep-merge (create-tag-index site-data)))]
    (realize-pages site-data config)))

(defn generate-page-list
  "Creates a seq of maps which each represent a page in the website."
  [realized-site-data]
  {:pre [(map? realized-site-data)] :post [(seq? %)]}
  (->> realized-site-data
       ;; If key is vector, then it is a page
       (reduce-kv (fn [page-list id v]
                    (if (vector? id)
                      ;; Add the page id to the map
                      (conj page-list (assoc v :id id))
                      page-list)) [])
       ;; Add get-site-data helper function to each page
       (map #(assoc % :get-site-data (gen-get-site-data realized-site-data)))))

(defn generate-site-index
  "Creates a map where the keys are relative URIs and the values are maps
  representing the web page. This datastructure is for the Stasis library."
  [page-list render-webpage debug?]
  {:pre [(seq? page-list) (fn? render-webpage)] :post [(map? %)]}
  (->> page-list
       (map (fn [page] (when-let [render-result (render-webpage page)]
                         [(:uri page)
                          (fn [_]
                            (when debug?
                              (log/info "⚡🐈 Rendering page:\n"
                                        (with-out-str (pprint page))))
                            (str "<!DOCTYPE html>"
                                 (hiccup/html render-result)))])))
       (into {})))

(def valid-item-tags
  [:type :image :url :title :link :description "content:encoded" :author
   :category :comments :enclosure :guid :pubDate :source])

(def required-item-tags [:title :description])

(def valid-channel-tags
  [:title :link :feed-url :description :category :cloud :copyright :docs :image
   :language :lastBuildDate :managingEditor :pubDate :rating :skipDays :skipHours
   :ttl :webMaster])

(def required-channel-tags [:title :link :description])

(defn create-rss-feed
  "Creates a string of XML that is a valid RSS feed"
  ;; TODO: make sure that clj-rss baked in PermaLink=false is ok
  [realized-site-data {:keys [link] :as rss-opts}]
  {:pre [(map? realized-site-data) (map? rss-opts)]
   :post [(string? %)]}
  (let [rss-items
        (for [{:keys [rss? uri] :as webpage} (vals realized-site-data)
              :when rss?]
          (-> webpage
              (select-keys valid-item-tags)
              (assoc :guid (str link uri))))]
    (try (apply rss/channel-xml
                rss-opts
                rss-items)
      (catch Exception e
        (let [msg (.getMessage e)]
          (log/error "Unable to create RSS feed.")
          (when (re-find #"is a required element$" msg)
            (log/error "The :rss-opts map must contain all of these keys:"
                       required-channel-tags))
          (when (re-find #"^unrecognized tags in channel" msg)
            (log/error "The :rss-opts map can only contain these keys: "
                       valid-channel-tags))
          (when (re-find #"^item" msg)
            (log/error "A webpage marked with :rss? true must have at least one of these keys:"
                       required-item-tags)))
        (throw e)))))

(defn export-site-index
  [site-index static-dir output-dir]
  (fs/create-dirs output-dir)
  (stasis/empty-directory! output-dir)
  (stasis/export-pages site-index output-dir)
  (when static-dir
    (util/ensure-static-dir static-dir)
    (fs/copy-tree static-dir output-dir)))

