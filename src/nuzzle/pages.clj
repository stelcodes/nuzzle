(ns nuzzle.pages
  ;; (:use clojure.stacktrace)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.set :as set]
   [expound.alpha :as expound]
   [nuzzle.schemas :as schemas]
   [nuzzle.util :as util]
   ;; Register spell-spec expound helpers after requiring expound.alpha
   [spell-spec.expound]))

(defn remove-draft-pages
  {:malli/schema [:=> [:cat schemas/pages] schemas/pages]}
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
  {:malli/schema [:=> [:cat schemas/pages [:? schemas/tag-pages-opts]] schemas/pages]}
  [pages & {:keys [parent-url create-title render-page]
            :or {parent-url [:tags] create-title #(->> % name (str "Tag "))}}]
  (assert render-page "Must provide :render-page function in :tag-pages opts map")
  (->> pages
       ;; Create a map shaped like {tag-kw #{url url ...}}
       (reduce-kv
        (fn [acc url {:nuzzle/keys [tags] :as _page}]
          (if tags
            (merge-with into acc (zipmap tags (repeat #{url})))
            acc))
        {})
       ;; Then change each entry into a proper page entry
       (reduce-kv
        (fn [acc tag urlset]
          (assoc acc (conj parent-url tag) {:nuzzle/index urlset
                                            :nuzzle/render-page render-page
                                            :nuzzle/title (create-title tag)}))
        {})
       (util/deep-merge pages)))

(defn validate-pages
  {:malli/schema [:=> [:cat schemas/pages] schemas/pages]}
  [pages]
  (if (s/valid? :nuzzle/user-pages pages)
    pages
    (do (expound/expound :nuzzle/user-pages pages {:theme :figwheel-theme
                                                   :print-specs? false})
      (throw (ex-info (str "Invalid pages:"
                           (->> pages
                                (s/explain-str :nuzzle/user-pages)
                                (re-find #"failed:(.*)") second))
                      {})))))

(defn create-get-pages
  "Create the helper function get-pages from the transformed pages. This
  function takes a pages key and returns the corresponding value with added
  key :nuzzle/get-pages with value get-pages function attached."
  {:malli/schema [:=> [:cat schemas/enriched-pages] fn?]}
  [pages]
  {:pre [(map? pages)] :post [(fn? %)]}
  (fn get-pages
    ([]
     (->> pages vals (map #(assoc % :nuzzle/get-pages get-pages))))
    ([url & {:keys [children?]}]
     ;; Return a single page map
     (if-let [page (pages url)]
       (if-not children?
         (assoc page :nuzzle/get-pages get-pages)
         (reduce-kv (fn [acc maybe-child-url maybe-child-page]
                      (if (util/child-url? url maybe-child-url)
                        (conj acc (assoc maybe-child-page :nuzzle/get-pages get-pages))
                        acc))
                    '()
                    pages))
       (throw (ex-info (str "get-pages error: Nonexistent URL " (pr-str url))
                       {:url url :pages pages}))))))

(defn transform-pages
  "Creates fully transformed pages with or without drafts."
  {:malli/schema [:=> [:cat schemas/pages] schemas/enriched-pages]}
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
            (if (= :children index)
              ;; Add index of all pages directly "beneath" this page
              (reduce (fn [acc maybe-child-url]
                        (cond-> acc
                          (util/child-url? url maybe-child-url) (conj maybe-child-url)))
                      #{}
                      all-urls)
              ;; Remove any URLs from index that don't exist
              (set/intersection index (set all-urls))))
          (update-pages [pages]
            (reduce-kv
             (fn [acc url {:nuzzle/keys [updated index] :as page}]
               (assoc acc url
                      (cond-> page
                        true (assoc :nuzzle/url url)
                        true (update :nuzzle/render-content update-render-content)
                        updated (update :nuzzle/updated #(cond-> %
                                                           (= java.util.Date (class %)) (.toInstant)))
                        index (update :nuzzle/index (partial update-index url (keys pages))))))
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
  {:malli/schema [:=> [:cat schemas/alt-pages [:? schemas/load-pages-opts]] schemas/pages]}
  [pages & {:keys [remove-drafts tag-pages]}]
  (let [resolved-pages (if (var? pages) (var-get pages) pages)
        pages (if (fn? resolved-pages) (resolved-pages) resolved-pages)]
    (cond-> pages
      true validate-pages
      remove-drafts remove-draft-pages
      tag-pages (add-tag-pages tag-pages)
      true transform-pages)))
