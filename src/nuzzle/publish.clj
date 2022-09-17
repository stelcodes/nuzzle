(ns nuzzle.publish
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [clojure.set :as set]
   [clojure.string :as str]
   [nuzzle.pages :as pages]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.util :as util]
   ;; clojure.data.xml uses namespaced keywords for XML namespaces, explicitly
   ;; defining them is the easiest way to avoid clj-kondo warnings
   [xmlns.http%3A%2F%2Fwww.sitemaps.org%2Fschemas%2Fsitemap%2F0.9 :as-alias sm]
   [xmlns.http%3a%2f%2fwww.w3.org%2f2005%2fAtom :as-alias atom]))

;; clojure/data.xml forbids default XML namespaces which is disappointing but
;; the alternative is clojure.xml which is under-documented and doesn't escape
;; XML characters ;-;

(defn create-sitemap
  ;; http://www.sitemaps.org/protocol.html
  [pages & {:keys [base-url]}]
   (xml/emit-str
    {:tag ::sm/urlset
     :content
     (for [{:nuzzle/keys [url updated]} (vals pages)
           :let [abs-url (str base-url (util/stringify-url url))]]
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
  [pages & {:keys [title author base-url logo icon subtitle]}]
  (xml/emit-str
   {:tag ::atom/feed
    :content
    (into [{:tag ::atom/title
            :content title}
           {:tag ::atom/id
            :content (if (-> base-url last (= \/)) base-url (str base-url "/"))}
           {:tag ::atom/link
            :attrs {:rel "self" :href (str base-url "/feed.xml")}}
           {:tag ::atom/updated
            :content (str (util/now-trunc-sec))}
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
          (for [{:nuzzle/keys [url updated summary author title render-content feed?] :as page} (vals pages)
                :let [content (when render-content (render-content page))
                      abs-url (str base-url (util/stringify-url url))]
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

(defn export-pages [pages publish-dir]
  (doseq [{:nuzzle/keys [url render-page] :as page} (vals pages)
          :let [str-url (util/stringify-url url)
                file-dir (str publish-dir str-url)
                file-path (str file-dir "index.html")]]
    (log/log-rendering-page url)
    (fs/create-dirs file-dir)
    (spit file-path (-> page render-page hiccup/hiccup->html-document))))

(defn publish-site
  [pages & {:keys [base-url publish-dir overlay-dir atom-feed sitemap? remove-drafts?]
            :or {publish-dir "dist" sitemap? true remove-drafts? true}}]
  (assert (or (and (not sitemap?) (not atom-feed)) base-url)
          "Must provide a :base-url optional arg in order to create sitemap or atom feed")
  (let [publish-dir (str/replace publish-dir #"/$" "")
        pages (pages/load-pages pages :remove-drafts? remove-drafts?)
        last-snapshot (util/create-dir-snapshot publish-dir)]
    (log/log-publish-start publish-dir)
    (fs/create-dirs publish-dir)
    (fs/delete-tree publish-dir)
    (export-pages pages publish-dir)
    (when overlay-dir
      (log/log-overlay-dir overlay-dir)
      (util/ensure-overlay-dir overlay-dir)
      (fs/copy-tree overlay-dir publish-dir))
    (when atom-feed
      (let [feed-file (fs/file publish-dir "feed.xml")]
        (log/log-feed feed-file)
        (spit feed-file (str (create-atom-feed pages atom-feed) \newline))))
    (when sitemap?
      (let [sitemap-file (fs/file publish-dir "sitemap.xml")]
        (log/log-sitemap sitemap-file)
        (spit sitemap-file (str (create-sitemap pages :base-url base-url) \newline))))
    (->> publish-dir
         util/create-dir-snapshot
         (util/create-dir-diff last-snapshot)
         log/report-dir-diff)
    (log/log-publish-end)))
