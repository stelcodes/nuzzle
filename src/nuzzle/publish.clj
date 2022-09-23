(ns nuzzle.publish
  (:require
   [babashka.fs :as fs]
   [clojure.data.xml :as xml]
   [clojure.string :as str]
   [nuzzle.pages :as pages]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.schemas :as schemas]
   [nuzzle.util :as util]
   ;; clojure.data.xml uses namespaced keywords for XML namespaces, explicitly
   ;; defining them is the easiest way to avoid clj-kondo warnings
   [xmlns.http%3A%2F%2Fwww.sitemaps.org%2Fschemas%2Fsitemap%2F0.9 :as-alias sm]
   [xmlns.http%3a%2f%2fwww.w3.org%2f2005%2fAtom :as-alias atom]))

;; clojure/data.xml forbids default XML namespaces which is disappointing but
;; the alternative is clojure.xml which is under-documented and doesn't escape
;; XML characters ;-;

(defn render-hiccup-xml
  {:malli/schema [:=> [:cat vector?] string?]}
  [hiccup-xml]
  (-> hiccup-xml
      xml/sexp-as-element
      (xml/indent-str :encoding "UTF-8")))

(defn create-sitemap
  ;; http://www.sitemaps.org/protocol.html
  {:malli/schema [:=> [:cat schemas/enriched-pages [:? [:map [:base-url schemas/http-url]]]] vector?]}
  [pages & {:keys [base-url]}]
  (render-hiccup-xml
   [::sm/urlset
    (for [{:nuzzle/keys [url updated]} (vals pages)
          :let [abs-url (str base-url (util/stringify-url url))]]
      [::sm/url
       [::sm/loc abs-url]
       (when updated
         [::sm/lastmod (str updated)])])]))

(defn create-author-element
  {:malli/schema [:=> [:cat schemas/author] vector?]}
  [{:keys [name email url] :as author}]
  (when-not name (throw (ex-info (str "Invalid author keyword: " (pr-str author)) {:author author})))
  [::atom/author
   [::atom/name name]
   (when email
     [::atom/email email])
   (when url
     [::atom/uri url])])

(defn create-atom-feed
  {:malli/schema [:=> [:cat schemas/enriched-pages [:? (conj schemas/atom-feed [:base-url schemas/http-url])]]
                  vector?]}
  [pages & {:keys [title author base-url logo icon subtitle]}]
  (render-hiccup-xml
   [::atom/feed
    [::atom/title title]
    [::atom/id (if (-> base-url last (= \/)) base-url (str base-url "/"))]
    [::atom/link {:rel "self" :href (str base-url "/feed.xml")}]
    [::atom/updated (str (util/now-trunc-sec))]
    (when-let [feed-author author]
      (create-author-element feed-author))
    (when icon [::atom/icon icon])
    (when logo [::atom/logo logo])
    (when subtitle [::atom/subtitle subtitle])
    (for [{:nuzzle/keys [url updated summary author title render-content feed?] :as page} (vals pages)
          :let [content (when render-content (render-content page))
                abs-url (str base-url (util/stringify-url url))]
          :when feed?]
      [::atom/entry
       [::atom/title title]
       [::atom/id abs-url]
       (when content [::atom/content {:type "html"} (hiccup/hiccup->html content)])
       (when updated [::atom/updated (str updated)])
       (when summary [::atom/summary summary])
       (when author (create-author-element author))])]))

(defn export-pages
  {:malli/schema [:=> [:cat schemas/enriched-pages string?] nil?]}
  [pages publish-dir]
  (doseq [{:nuzzle/keys [url render-page] :as page} (vals pages)
          :let [str-url (util/stringify-url url)
                file-dir (str publish-dir str-url)
                file-path (str file-dir "index.html")]]
    (log/log-rendering-page url)
    (fs/create-dirs file-dir)
    (spit file-path (-> page render-page hiccup/hiccup->html-document))))

(defn publish-site
  {:malli/schema [:=> [:cat schemas/enriched-pages [:? schemas/publish-opts]] nil?]}
  [pages & {:keys [base-url publish-dir overlay-dir atom-feed sitemap? remove-drafts tag-pages]
            :or {publish-dir "dist" remove-drafts true}}]
  (assert (or (and (not sitemap?) (not atom-feed)) base-url)
          "Must provide a :base-url optional arg in order to create sitemap or atom feed")
  (let [publish-dir (str/replace publish-dir #"/$" "")
        pages (pages/load-pages pages {:remove-drafts remove-drafts :tag-pages tag-pages})
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
        (spit feed-file (create-atom-feed pages atom-feed))))
    (when sitemap?
      (let [sitemap-file (fs/file publish-dir "sitemap.xml")]
        (log/log-sitemap sitemap-file)
        (spit sitemap-file (create-sitemap pages {:base-url base-url}))))
    (->> publish-dir
         util/create-dir-snapshot
         (util/create-dir-diff last-snapshot)
         log/report-dir-diff)
    (log/log-publish-end)))
