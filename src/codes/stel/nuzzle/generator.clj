(ns codes.stel.nuzzle.generator
  (:require [babashka.fs :as fs]
            [clj-rss.core :as rss]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [codes.stel.nuzzle.hiccup :as hiccup]
            [codes.stel.nuzzle.log :as log]
            [codes.stel.nuzzle.util :as util]
            [markdown.core :refer [md-to-html-string]]
            [markdown.transformers :refer [transformer-vector]]
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

(defn validate-site-data [site-data]
  (let [missing-homepage? (not (some #(= [] (:id %)) site-data))]
    (cond
     missing-homepage?
     (throw (ex-info "Site data is missing homepage (webpage map with an :id of [])" {}))
     :else site-data)))

(defn load-site-data
  "Read the site-data EDN file and validate it."
  [site-data]
  {:pre [(string? site-data)] :post [(map? %)]}
  (->
   (try
     (-> site-data
         slurp
         edn/read-string)
     (catch Throwable _
       (throw (ex-info
               (str "Site data file: " site-data " could not be read. Make sure the file exists and the contents are a valid EDN vector.")
               {:path site-data}))))
   validate-site-data
   convert-site-data-to-map))

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

(defn maybe-sh [& args]
  (try (apply sh args)
    (catch Exception _ nil)))

(defn highlight-md-code [text state]
  (log/info (clojure.pprint/pprint {:text text :state state}))
  (if-not true #_(= :codeblock state)
   [text state]
   (let [commands [["chroma" "--html" text]]]
    (loop [[next-command & rest-commands] commands]
      (if-not next-command
        [text state]
        (let [{:keys [exit out]} (maybe-sh next-command)]
          (log/info "TEXT: " text)
          (if (= 0 exit)
            (do (log/info "successful md code conversion!") (log/info out) [out state])
            (do (log/info next-command "didn't work") (recur rest-commands)))))))))

(defn process-markdown-file [file]
  (md-to-html-string (slurp file)
                     :heading-anchors true
                     :reference-style-links true
                     :footnotes true
                     :parse-meta? false
                     :custom-transformers [highlight-md-code]))

(comment (sh "chrddoma" "--help"))

(defn create-render-content-fn
  "Create a function that turned the :content file into html, wrapped with the
  hiccup raw identifier."
  [id content]
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
           (hiccup/raw (process-markdown-file content-file)))
         ;; If extension not recognized, throw Exception
         :else (throw (ex-info (str "Filetype of content file " content " for id " id " not recognized")
                      {:id id :content content}))))
      ;; If content-file is defined but it can't be found, throw an Exception
      (throw (ex-info (str "Resource " content " for id " id " not found")
                      {:id id :content content})))))

(defn realize-pages
  "Adds :uri, :render-content keys to each page in the site-data."
  [site-data]
  {:pre [map? site-data]}
  (reduce-kv
   (fn [m id {:keys [content uri] :as v}]
     (if (vector? id)
       (assoc m id
              (merge v {:uri (or uri (util/id->uri id))
                        :render-content
                        (create-render-content-fn id content)}))
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
  [site-data remove-drafts?]
  {:pre [(map? site-data) (boolean? remove-drafts?)]}
  ;; Allow users to define their own overrides via deep-merge
  (let [site-data (if remove-drafts?
                      (remove-drafts site-data)
                      site-data)]
    (->> site-data
         ;; Make sure there is a root index.html file
         ;; (util/deep-merge {[] {:uri "/"}})
         (util/deep-merge (create-group-index site-data))
         (util/deep-merge (create-tag-index site-data))
         (realize-pages))))

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

(defn create-rss-feed
  "Creates a string of XML that is a valid RSS feed"
  ;; TODO: make sure that clj-rss baked in PermaLink=false is ok
  [realized-site-data {:keys [author link] :as rss-opts}]
  {:pre [(map? realized-site-data) (map? rss-opts) (string? author)]
   :post [(string? %)]}
  (when rss-opts
    (apply rss/channel-xml
           (select-keys rss-opts [:title :description :link])
           (->>
            (for [{:keys [uri title rss]} (vals realized-site-data)]
              (when rss
                (-> {:title (or title "Untitled") :guid (str link uri) :author author}
                    (merge (when (map? rss) rss))
                    util/remove-nil-values)))
            (remove nil?)))))

(defn export-site-index
  [site-index static-dir output-dir]
  (fs/create-dirs output-dir)
  (stasis/empty-directory! output-dir)
  (stasis/export-pages site-index output-dir)
  (when static-dir
    (util/ensure-static-dir static-dir)
    (fs/copy-tree static-dir output-dir)))

