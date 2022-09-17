(ns nuzzle.publish
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [nuzzle.pages :as pages]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.util :as util]
   [stasis.core :as stasis]
   ;; clojure.data.xml uses namespaced keywords for XML namespaces, explicitly
   ;; defining them is the easiest way to avoid clj-kondo warnings
   [xmlns.http%3A%2F%2Fwww.sitemaps.org%2Fschemas%2Fsitemap%2F0.9 :as-alias sm]
   [xmlns.http%3a%2f%2fwww.w3.org%2f2005%2fAtom :as-alias atom]))

;; clojure/data.xml forbids default XML namespaces which is disappointing but
;; the alternative is clojure.xml which is under-documented and doesn't escape
;; XML characters ;-;

(defn create-sitemap
  "Assumes config is transformed and drafts have already been removed from
  rendered-site-index."
  ;; http://www.sitemaps.org/protocol.html
  [pages rendered-site-index & {:keys [base-url]}]
   (xml/emit-str
    {:tag ::sm/urlset
     :content
     (for [[url-str _hiccup] rendered-site-index
           :let [url-vec (util/vectorize-url url-str)
                 {:nuzzle/keys [updated]} (get pages url-vec)
                 abs-url (str base-url url-str)]]
       {:tag ::sm/url
        :content
        [{:tag ::sm/loc
          :content [abs-url]}
         (when updated
           {:tag ::sm/lastmod
            :content (str updated)})]})}
    {:encoding "UTF-8"}))

(defn create-author-element
  [{:keys [name email url] :as author}]
  (when-not name (throw (ex-info (str "Invalid author keyword: " (pr-str author)) {:author author})))
  {:tag ::atom/author
   :content [{:tag ::atom/name
              :content name}
             (when email
               {:tag ::atom/email
                :content email})
             (when url
               {:tag ::atom/uri
                :content url})]})

(defn create-atom-feed
  "The optional test-ops map can make build deterministic by setting
  :deterministic? true"
  [pages rendered-site-index & {:keys [title author base-url logo icon subtitle deterministic?]}]
  (xml/emit-str
   {:tag ::atom/feed
    :content
    (into [{:tag ::atom/title
            :content title}
           {:tag ::atom/id
            :content (if (-> base-url last (= \/)) base-url (str base-url "/"))}
           {:tag ::atom/link
            :attrs {:rel "self" :href (str base-url "/feed.xml")}}
           (when-not deterministic?
             {:tag ::atom/updated
              :content (str (util/now-trunc-sec))})
           (when-let [feed-author author]
             (create-author-element feed-author))
           (when icon
             {:tag ::atom/icon
              :content icon})
           (when logo
             {:tag ::atom/logo
              :content logo})
           (when subtitle
             {:tag ::atom/subtitle
              :content subtitle})]
          (for [[url-str _] rendered-site-index
                :let [url-vec (util/vectorize-url url-str)
                      {:nuzzle/keys [updated summary author title render-content feed?]} (get pages url-vec)
                      content (render-content)
                      abs-url (str base-url url-str)]
                :when feed?]
            {:tag ::atom/entry
             :content [{:tag ::atom/title
                        :content title}
                       {:tag ::atom/id
                        :content abs-url}
                       (when content
                         {:tag ::atom/content
                          :attrs {:type "html"}
                          :content (hiccup/hiccup->html content)})
                       (when updated
                         {:tag ::atom/updated
                          :content (str updated)})
                       (when summary
                         {:tag ::atom/summary
                          :content summary})
                       (when author (create-author-element author))]}))}
   {:encoding "UTF-8"}))

(defn publish-site
  "The optional test-ops map can make build deterministic by setting
  :deterministic? true"
  [pages & {:keys [publish-dir overlay-dir atom-feed sitemap? deterministic? remove-drafts?]
            :or {publish-dir "dist" sitemap? true remove-drafts? true} :as opts}]
  (let [pages (pages/load-pages pages :remove-drafts? remove-drafts?)
        rendered-site-index (pages/create-stasis-pages pages)]
    (log/log-publish-start publish-dir)
    (fs/create-dirs publish-dir)
    (stasis/empty-directory! publish-dir)
    (stasis/export-pages rendered-site-index publish-dir)
    (when overlay-dir
      (log/log-overlay-dir overlay-dir)
      (util/ensure-overlay-dir overlay-dir)
      (fs/copy-tree overlay-dir publish-dir))
    (when atom-feed
      (let [feed-file (fs/file publish-dir "feed.xml")]
        (log/log-feed feed-file)
        (spit feed-file (str (create-atom-feed pages rendered-site-index (assoc atom-feed :deterministic? deterministic?)) \newline))))
    (when sitemap?
      (let [sitemap-file (fs/file publish-dir "sitemap.xml")]
        (log/log-sitemap sitemap-file)
        (spit sitemap-file (str (create-sitemap pages rendered-site-index opts) \newline))))
    (log/log-publish-end)))
