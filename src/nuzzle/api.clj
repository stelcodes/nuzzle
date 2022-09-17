(ns nuzzle.api
  (:require
   [cybermonday.core :as cm]
   [lambdaisland.deep-diff2 :as ddiff]
   [nuzzle.pages :as pages]
   [nuzzle.publish :as publish]
   [nuzzle.log :as log]
   [nuzzle.server :as server]
   [nuzzle.util :as util]))

(defn transform
  "Allows the user to visualize the site data after Nuzzle's modifications."
  [pages]
  (log/info "🔍🐈 Returning transformed config")
  (pages/load-pages pages))

(defn transform-diff
  "Pretty prints the diff between the config in nuzzle.edn and the config after
  Nuzzle's transformations."
  [pages]
  (let [transformed-pages (pages/load-pages pages)]
    (log/info "🔍🐈 Printing Nuzzle's config transformations diff")
    (ddiff/pretty-print (ddiff/diff pages transformed-pages))))

(defn publish
  "Publishes the website to :nuzzle/publish-dir. The overlay directory is
  overlayed on top of the publish directory after the web pages have been
  published."
  [pages & {:as opts}]
  (publish/publish-site pages opts))

(defn serve
  "Starts a server using http-kit for development."
  [pages & {:as opts}]
  (server/start-server pages opts))

(defn parse-md [md-str]
  (let [lower-code-block
        (fn lower-code-block [[_tag-name {:keys [language]} code]]
          [:pre
           [:code
            {:lang language
             :class ["code-block" (when language (str "language-" language))]}
            code]])
        lower-fns {:markdown/fenced-code-block lower-code-block
                   :markdown/indented-code-block lower-code-block}
        ;; Avoid the top level div [:div {} content...]
        [_ _ & hiccup] (cm/parse-body md-str {:lower-fns lower-fns})]
    hiccup))

(defn add-tag-pages
  "Add pages page entries for pages that index all the pages which are tagged
  with a particular tag. Each one of these tag index pages goes under the
  /tags/ subdirectory"
  [pages render-page & {:keys [parent-url title-fn]
                        :or {parent-url [:tags] title-fn #(->> % name (str "Tag "))}}]
  (->> pages
       ;; Create a map shaped like {tag-kw #{url url ...}}
       (reduce-kv
        (fn [acc url {:nuzzle/keys [tags] :as _page}]
          (if tags
            (merge-with into acc (zipmap tags (repeat #{url})))
            acc))
        {})
       ;; Then change each entry into a proper page entry
       (reduce-kv
        (fn [acc tag urlset]
          (assoc acc (conj parent-url tag) {:nuzzle/index urlset
                                            :nuzzle/render-page render-page
                                            :nuzzle/title (title-fn tag)}))
        {})
       (util/deep-merge pages)))
