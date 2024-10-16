(ns nuzzle.pages
  ;; (:use clojure.stacktrace)
  (:require
   [clojure.set :as set]
   [malli.core :as m]
   [malli.dev.pretty :as mp]
   [nuzzle.schemas :as schemas]
   [nuzzle.util :as util]))

(defn remove-draft-pages
  {:malli/schema [:-> schemas/pages schemas/pages]}
  [pages]
  (reduce-kv (fn [acc url {:nuzzle/keys [draft] :as page}]
               (if draft
                 acc
                 (assoc acc url page)))
             {}
             pages))

(defn add-tag-pages
  "Add pages page entries for pages that index all the pages which are tagged
  with a particular tag. Each one of these tag index pages goes under the
  /tags/ subdirectory"
  {:malli/schema [:-> schemas/pages schemas/pages]}
  [pages]
  (let [[template-url template-page] (util/seek #(= :nuzzle/tag (-> % key last)) pages)]
    (if template-url
      (->> pages
           ;; Create a map shaped like {:tag #{[:some :url] [:another :url] ...}}
           (reduce-kv
            (fn [acc url {:nuzzle/keys [tags] :as _page}]
              (if tags
                (merge-with into acc (zipmap tags (repeat #{url})))
                acc))
            {})
           ;; Then convert each tag into a page entry
           (reduce-kv
            (fn [acc tag urls-with-tag]
              (assoc acc (conj (pop template-url) tag)
                     (assoc template-page :nuzzle/index urls-with-tag)))
            {})
           ;; Could just use normal merge since new pages shouldn't be in users page map
           (util/deep-merge pages))
      pages)))

(defn validate-pages
  {:malli/schema [:-> schemas/pages nil?]}
  [pages]
  (let [pages-with-url (reduce-kv (fn [acc url page] (conj acc (assoc page :nuzzle/url url)))
                                  []
                                  pages)]
    (doall (map #(when (not (m/validate schemas/validate-page %))
                   (mp/explain schemas/validate-page %)
                   (throw (ex-info (str "Invalid pages:" %)
                                   {})))
                pages-with-url))))

(defn create-get-pages
  "Create the helper function get-pages from the transformed pages. This
  function takes a pages key and returns the corresponding value with added
  key :nuzzle/get-pages with value get-pages function attached."
  {:malli/schema [:-> schemas/enriched-pages fn?]}
  [pages]
  {:pre [(map? pages)] :post [(fn? %)]}
  (fn get-pages
    ([]
     ;; Return a list of all pages
     (apply get-pages (keys pages)))
    ([urlset]
     ;; Return a list of pages of specified urls or a single page map
     (reduce (fn [acc url]
               (if (contains? pages url)
                 (conj acc (-> pages (get url) (assoc :nuzzle/url url :nuzzle/get-pages get-pages)))
                 (throw (ex-info (str "get-pages error: Nonexistent URL " (pr-str url))
                                 {:url url}))))

             []
             urlset))))

(defn transform-pages
  "Creates fully transformed pages with or without drafts."
  {:malli/schema [:-> schemas/pages schemas/enriched-pages]}
  [pages]
  {:pre [(map? pages)] :post [#(map? %)]}
  (letfn [(update-render-content [render-content]
            (if render-content
              (fn wrap-render-content [& {:as page}]
                (try (render-content page)
                     (catch clojure.lang.ArityException _
                       (render-content))))
              (constantly nil)))
          (update-index [url all-urls index]
            (if index
              ;; Remove any URLs from index that don't exist
              (set/intersection index (set all-urls))
              ;; Add index of all pages directly "beneath" this page
              (reduce (fn [acc maybe-child-url]
                        (cond-> acc
                          (util/child-url? url maybe-child-url) (conj maybe-child-url)))
                      #{}
                      all-urls)))
          (update-pages [pages]
            (reduce-kv
             (fn [acc url {:nuzzle/keys [title published updated index] :as page}]
               (assoc acc url
                      (cond-> page
                        true (assoc :nuzzle/url url)
                        true (update :nuzzle/render-content update-render-content)
                        (fn? title) (assoc :nuzzle/title (title url))
                        updated (update :nuzzle/updated #(cond-> %
                                                           (= java.util.Date (class %)) (.toInstant)))
                        published (update :nuzzle/published #(cond-> %
                                                               (= java.util.Date (class %)) (.toInstant)))
                        true (update :nuzzle/index (partial update-index url (keys pages))))))
             {} pages))
          (add-get-pages [pages]
            (let [get-pages (create-get-pages pages)]
              (update-vals pages #(assoc % :nuzzle/get-pages get-pages))))]
    (-> pages
        update-pages
        ;; Adding get-pages must come after all other transformations
        add-get-pages)))

(defn load-pages
  "Load a pages var or map and validate it."
  {:malli/schema [:-> schemas/alt-pages [:? schemas/load-pages-opts] schemas/pages]}
  [pages & {:keys [remove-drafts]}]
  (let [resolved-pages (if (var? pages) (var-get pages) pages)
        pages (if (fn? resolved-pages) (resolved-pages) resolved-pages)]
    (validate-pages pages)
    (cond-> pages
      remove-drafts remove-draft-pages
      true add-tag-pages
      true transform-pages)))
