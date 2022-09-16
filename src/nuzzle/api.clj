(ns nuzzle.api
  (:require [lambdaisland.deep-diff2 :as ddiff]
            [nuzzle.pages :as pages]
            [nuzzle.publish :as publish]
            [nuzzle.log :as log]
            [nuzzle.server :as server]))

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
  [pages & opts]
  (-> (pages/load-pages pages)
      (publish/publish-site opts)))

(defn serve
  "Starts a server using http-kit for development."
  [pages & opts]
  (server/start-server pages opts))
