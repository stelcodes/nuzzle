(ns site
  (:require [nuzzle.core :as nuzz]))

;; Create a pages map
(defn pages []
  {[]
   {:nuzzle/title "Homepage"
    :nuzzle/render-page (fn [{:nuzzle/keys [title] :as _page}]
                          [:html
                           [:h1 title]
                           [:a {:href [:about]}]] "About")}
   [:about]
   {:nuzzle/title "About"
    :nuzzle/render-content #(-> "md/about.md" slurp nuzz/parse-md)
    :nuzzle/render-page (fn [{:nuzzle/keys [render-content title] :as _page}]
                          [:html
                           [:h1 title]
                           (render-content)])}})

;; Start static site server + nREPL server with nuzzle.core/develop
;; Pass the pages as a var to get full hot-reloading capabilities!
;; The returned value is a function that stops both servers.
(defn develop [_]
  (nuzz/develop #'pages))

;; Publish the static site to ./dist
(defn publish [_]
  (nuzz/publish pages))
