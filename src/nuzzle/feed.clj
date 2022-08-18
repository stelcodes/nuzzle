(ns nuzzle.feed
  (:require
   [clojure.data.xml :as xml]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.util :as util]
   ;; clojure.data.xml uses namespaced keywords for XML namespaces, explicitly
   ;; defining them is the easiest way to avoid clj-kondo warnings
   [xmlns.http%3a%2f%2fwww.w3.org%2f2005%2fAtom :as-alias atom]))

;; clojure.xml is under-documented, doesn't escape XML characters, and produces
;; awful error messages. Stay away from clojure.xml ;-;

(defn create-author-element
  [{:nuzzle/keys [author-registry] :as _config} author-kw]
  (let [{:keys [name email url]} (get author-registry author-kw)]
    (when-not name (throw (ex-info (str "Invalid author keyword: " author-kw) {})))
    {:tag ::atom/author
     :content [{:tag ::atom/name
                :content name}
               (when email
                 {:tag ::atom/email
                  :content email})
               (when url
                 {:tag ::atom/uri
                  :content url})]}))

(defn create-atom-feed
  "The optional test-ops map can make build deterministic by setting
  :deterministic? true"
  [{:nuzzle/keys [base-url atom-feed] :as config} rendered-site-index & {:keys [deterministic?]}]
  (xml/emit-str
   {:tag ::atom/feed
    :content
    (into [{:tag ::atom/title
            :content (:title atom-feed)}
           {:tag ::atom/id
            :content (if (-> base-url last (= \/)) base-url (str base-url "/"))}
           {:tag ::atom/link
            :attrs {:rel "self" :href (str base-url "/feed.xml")}}
           (when-not deterministic?
             {:tag ::atom/updated
              :content (str (util/now-trunc-sec))})
           (when-let [feed-author (:author atom-feed)]
             (create-author-element config feed-author))
           (when-let [icon (:icon atom-feed)]
             {:tag ::atom/icon
              :content icon})
           (when-let [logo (:logo atom-feed)]
             {:tag ::atom/logo
              :content logo})
           (when-let [subtitle (:subtitle atom-feed)]
             {:tag ::atom/subtitle
              :content subtitle})]
          (for [[rel-url _] rendered-site-index
                :let [page-key (util/url->page-key rel-url)
                      {:nuzzle/keys [updated summary author title content render-content feed?]} (get config page-key)
                      content-result (when (fn? render-content) (render-content))
                      abs-url (str base-url rel-url)]
                :when feed?]
            {:tag ::atom/entry
             :content [{:tag ::atom/title
                        :content title}
                       {:tag ::atom/id
                        :content abs-url}
                       (when content-result
                         {:tag ::atom/content
                          :attrs {:type "html"}
                          :content (str (hiccup/html content-result))})
                       (when (or updated content)
                         {:tag ::atom/updated
                          :content (str (or updated (util/path->last-mod-inst content)))})
                       (when summary
                         {:tag ::atom/summary
                          :content summary})
                       (when author (create-author-element config author))]}))}
   {:encoding "UTF-8"}))

(comment (println (create-atom-feed {:nuzzle/base-url "https://foo.com"
                                     :nuzzle/author-registry {:stel {:name "Stelly" :email "stel@email.com"}}
                                     :nuzzle/atom-feed {:title "Foobar Blog" :author :stel}
                                     [:hi-there] {:nuzzle/title "Hi"
                                                  :nuzzle/author :stel
                                                  :nuzzle/updated (util/now-trunc-sec)
                                                  :nuzzle/render-content #(str "<p>hi there</p>")
                                                  :nuzzle/feed? true}}
                           {"/" []
                            "/hi-there/" []
                            "/blog-posts/foobar/" []})))
