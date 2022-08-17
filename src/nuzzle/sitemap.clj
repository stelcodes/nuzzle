(ns nuzzle.sitemap
  (:require
   [clojure.data.xml :as xml]
   [nuzzle.util :as util]
   [xmlns.http%3A%2F%2Fwww.sitemaps.org%2Fschemas%2Fsitemap%2F0.9 :as-alias sm]))

;; http://www.sitemaps.org/protocol.html

(defn create-sitemap
  "Assumes config is transformed and drafts have already been removed from
  rendered-site-index."
  [{:nuzzle/keys [base-url] :as config} rendered-site-index]
   (xml/emit-str
    {:tag ::sm/urlset
     :content
     (for [[rel-url _hiccup] rendered-site-index
           :let [page-key (util/url->page-key rel-url)
                 {:nuzzle/keys [content updated]} (get config page-key)
                 abs-url (str base-url rel-url)]]
       {:tag ::sm/url
        :content
        [{:tag ::sm/loc
          :content [abs-url]}
         (when (or updated content)
           {:tag ::sm/lastmod
            :content (str (or updated (util/path->last-mod-inst content)))})]})}
    {:encoding "UTF-8"}))
