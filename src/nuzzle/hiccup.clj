(ns nuzzle.hiccup
  (:require [hiccup2.core :as hiccup]))

(def raw hiccup/raw)

(defmacro html [& args] `(hiccup/html ~@args))

(defmacro html-document [& args] `(str "<!DOCTYPE html>" (hiccup/html ~@args)))

(comment (str "<!DOCTYPE html>" (html [:html [:head [:title "Some Title"]] [:h1 "test"]])))

(comment (html-document [:html [:head [:title "Some Title"]] [:h1 "test"]]))
