(ns nuzzle.sitemap
  (:require
   [clojure.data.xml :as xml]
   [nuzzle.util :as util]))

;; Heavily inspired by cryogen-core.sitemap namespace in the cryogen-core project
;; https://github.com/cryogen-project/cryogen-core/blob/master/src/cryogen_core/sitemap.clj

;;http://www.sitemaps.org/protocol.html

(defn create-sitemap
  "Assumes realized site data is in map form and that drafts have already been
  removed from rendered-site-index."
  [{:keys [site-data location] :as _config} rendered-site-index]
   (xml/emit-str
    {:tag :urlset
     :attrs {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
     :content
     (for [[uri _hiccup] rendered-site-index
           :let [id (util/uri->id uri)
                 {:keys [modified]} (get site-data id)
                 page-location (str location uri)]]
       {:tag :url
        :content
        (remove nil?
                [{:tag :loc
                  :content [page-location]}
                 (when modified
                   {:tag :lastmod
                    :content [(util/format-simple-date modified)]})])})}
    {:encoding "UTF-8"}))
