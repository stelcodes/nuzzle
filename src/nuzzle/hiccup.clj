(ns nuzzle.hiccup
  (:require [rum.core :as rum]))

(defn raw [raw-html-str] [:<> {:dangerouslySetInnerHTML {:__html raw-html-str}}])

(defn html [& hiccup] (rum/render-static-markup hiccup))

(defn html-document [& hiccup] (str "<!DOCTYPE html>" (apply html hiccup)))

(comment (str "<!DOCTYPE html>" (html [:html [:head [:title "Some Title"]]])))

(comment (html-document [:html [:head [:title "Some Title"]] [:h1 "test"]]))
