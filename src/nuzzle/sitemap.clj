(ns nuzzle.sitemap
  (:require
   [clojure.data.xml :as xml]
   [nuzzle.util :as util]))

;; Heavily inspired by cryogen-core.sitemap namespace in the cryogen-core project
;; https://github.com/cryogen-project/cryogen-core/blob/master/src/cryogen_core/sitemap.clj

;;http://www.sitemaps.org/protocol.html

(defn create-sitemap
  "Assumes config is transformed and drafts have already been removed from
  rendered-site-index."
  [{:nuzzle/keys [base-url] :as config} rendered-site-index]
   (xml/emit-str
    {:tag :urlset
     :attrs {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
     :content
     (for [[rel-url _hiccup] rendered-site-index
           :let [page-key (util/url->page-key rel-url)
                 {:nuzzle/keys [updated]} (get config page-key)
                 abs-url (str base-url rel-url)]]
       {:tag :url
        :content
        [{:tag :loc
          :content [abs-url]}
         (when updated
           {:tag :lastmod
            :content (str updated)})]})}
    {:encoding "UTF-8"}))
