(ns nuzzle.schemas-test
  (:require
   [clojure.test :refer [deftest is]]
   [malli.core :as m]
   [nuzzle.schemas :as schemas]))

(deftest markdown
  (is (m/validate schemas/markdown-opts
                  {:syntax-highlighting
                   {:provider :chroma
                    :style "emacs"}
                   :shortcodes
                   {:foobar 'shortcodes/foobar
                    :foobaz 'shortcodes/foobaz}})))
