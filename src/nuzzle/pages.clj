(ns nuzzle.pages
  ;; (:use clojure.stacktrace)
  (:require
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.util :as util]
   ;; Register spell-spec expound helpers after requiring expound.alpha
   [spell-spec.expound]))

(defn add-tag-pages
  "Add pages page entries for pages that index all the pages which are tagged
  with a particular tag. Each one of these tag index pages goes under the
  /tags/ subdirectory"
  [pages render-page]
  (->> pages
       ;; Create a map shaped like {tag-kw #{url url ...}}
       (reduce-kv
        ;; For every key value pair in pages
        (fn [acc url {:nuzzle/keys [tags] :as _page}]
          (if tags
            ;; If entry is a page with tags, create a map with an entry for
            ;; every tag the page is tagged with and merge it into acc
            (merge-with into acc (zipmap tags (repeat #{url})))
            ;; if entry is an option or tagless page, don't change acc
            acc))
        {})
       ;; Then change each entry into a proper page entry
       (reduce-kv
        (fn [acc tag urlset]
          (assoc acc [:tags tag] {:nuzzle/index urlset
                                  :nuzzle/render-page render-page
                                  :nuzzle/title (str "#" (name tag))}))
        {})
       (util/deep-merge pages)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn remove-draft-pages [pages]
  (->> pages
       (remove :nuzzle/draft)
       (into {})))

(defn validate-pages [pages]
  (if (s/valid? :nuzzle/user-pages pages)
    pages
    (do (expound/expound :nuzzle/user-pages pages {:theme :figwheel-theme})
      (log/error "Encountered error in Nuzzle pages:")
      (throw (ex-info (str "Invalid Nuzzle pages! "
                           (re-find #"failed: .*" (s/explain-str :nuzzle/user-pages pages)))
                      {})))))

(defn create-get-pages
  "Create the helper function get-pages from the transformed pages. This
  function takes a pages key and returns the corresponding value with added
  key :nuzzle/get-pages with value get-pages function attached."
  [pages]
  {:pre [(map? pages)] :post [(fn? %)]}
  (fn get-pages
    ([& args]
     (if (empty? args)
       ;; If no args, return the whole pages
       (update-vals pages #(assoc % :nuzzle/get-pages get-pages))
       (reduce (fn [last-match arg]
                 (if (try (contains? last-match arg) (catch Throwable _ false))
                   (let [next-match (get last-match arg)]
                     (cond-> next-match
                       ;; Only add get-pages to returned value if it's a page entry map
                       (:nuzzle/url next-match) (assoc :nuzzle/get-pages get-pages)))
                   (throw (ex-info (str "get-pages error: key sequence "
                                        (-> args vec pr-str) " cannot be resolved")
                                   {:invalid-key arg}))))
               pages args)))))

(defn transform-pages
  "Creates fully transformed pages with or without drafts."
  [pages]
  {:pre [(map? pages)] :post [#(map? %)]}
  (letfn [(add-page-keys [pages]
            (reduce-kv
             (fn [acc ckey cval]
               (assoc acc ckey
                      (-> cval
                          (assoc :nuzzle/url ckey)
                          (update :nuzzle/render-content #(or % (constantly nil))))))
             {} pages))
          (add-get-pages [pages]
            (let [get-pages (create-get-pages pages)]
              (update-vals pages #(assoc % :nuzzle/get-pages get-pages))))]
    (-> pages
        add-page-keys
        ;; Adding get-pages must come after all other transformations
        add-get-pages)))

(defn load-pages
  "Load a pages var or map and validate it."
  [pages]
  (let [resolved-pages (if (var? pages) (var-get pages) pages)
        pages (if (fn? resolved-pages) (resolved-pages) resolved-pages)]
    (-> pages
        validate-pages
        (transform-pages))))

(defn create-site-index
  "Creates a map where the keys are relative URLs and the values are a string
  of HTML or a function that produces a string of HTML. This datastructure is
  defined by stasis."
  [pages & {:keys [lazy-render?]}]
  {:post [(map? %)]}
  (reduce-kv
   (fn [acc url {:nuzzle/keys [render-page] :as page}]
     (assoc acc (util/stringify-url url)
            (if lazy-render?
              ;; Turn the page's hiccup into HTML on the fly
              (fn [_]
                (log/log-rendering-page page)
                (-> page render-page hiccup/html-document))
              (-> page render-page hiccup/html-document))))
   {} pages))
