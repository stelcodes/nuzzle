(ns nuzzle.pages
  ;; (:use clojure.stacktrace)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.set :as set]
   [expound.alpha :as expound]
   [nuzzle.schemas]
   [nuzzle.util :as util]
   ;; Register spell-spec expound helpers after requiring expound.alpha
   [spell-spec.expound]))

(defn remove-draft-pages [pages]
  (let [draft-urls (->> pages
                        vals
                        (filter :nuzzle/draft?)
                        (map :nuzzle/url)
                        set)]
    (reduce-kv (fn [acc url {:nuzzle/keys [draft? index] :as page}]
                 (if draft?
                   acc
                   (assoc acc url
                          (cond-> page
                            index (update :nuzzle/index #(set/difference % draft-urls))))))
               {}
               pages)))

(defn validate-pages [pages]
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
  [pages]
  {:pre [(map? pages)] :post [#(map? %)]}
  (letfn [(update-render-content [render-content]
            (if render-content
              (fn wrap-render-content [& {:as page}]
                (try (render-content page)
                  (catch clojure.lang.ArityException _
                    (render-content))))
              (constantly nil)))
          (add-page-keys [pages]
            (reduce-kv
             (fn [acc url {:nuzzle/keys [updated index] :as page}]
               (assoc acc url
                      (cond-> page
                        true (assoc :nuzzle/url url)
                        true (update :nuzzle/render-content update-render-content)
                        updated (update :nuzzle/updated #(cond-> %
                                                           (= java.util.Date (class %)) (.toInstant)))
                        index (update :nuzzle/index
                                      #(if (not= :children %) %
                                         (reduce (fn [acc url2]
                                                   (cond-> acc
                                                     (util/child-url? url url2) (conj url2)))
                                                 #{}
                                                 (keys pages)))))))
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
  [pages & {:keys [remove-drafts?]}]
  (let [resolved-pages (if (var? pages) (var-get pages) pages)
        pages (if (fn? resolved-pages) (resolved-pages) resolved-pages)
        handle-drafts #(cond-> % remove-drafts? remove-draft-pages)]
    (-> pages
        validate-pages
        handle-drafts
        transform-pages)))
