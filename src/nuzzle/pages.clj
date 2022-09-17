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

(defn create-get-page
  "Create the helper function get-page from the transformed pages. This
  function takes a pages key and returns the corresponding value with added
  key :nuzzle/get-page with value get-page function attached."
  [pages]
  {:pre [(map? pages)] :post [(fn? %)]}
  (fn get-page
    ([url]
     (if (= url :all)
       ;; Return the whole pages map
       (update-vals pages #(assoc % :nuzzle/get-page get-page))
       ;; Return a single page map
       (if-let [page (pages url)]
         (assoc page :nuzzle/get-page get-page)
         (throw (ex-info (str "get-page error: Nonexistent URL " (pr-str url))
                         {:url url :pages pages})))))))

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
          (add-get-page [pages]
            (let [get-page (create-get-page pages)]
              (update-vals pages #(assoc % :nuzzle/get-page get-page))))]
    (-> pages
        add-page-keys
        ;; Adding get-page must come after all other transformations
        add-get-page)))

(defn load-pages
  "Load a pages var or map and validate it."
  [pages]
  (let [resolved-pages (if (var? pages) (var-get pages) pages)
        pages (if (fn? resolved-pages) (resolved-pages) resolved-pages)]
    (-> pages
        validate-pages
        transform-pages)))

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
                (-> page render-page hiccup/hiccup->html-document))
              (-> page render-page hiccup/hiccup->html-document))))
   {} pages))
