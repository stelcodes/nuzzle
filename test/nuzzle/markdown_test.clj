(ns nuzzle.markdown-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.markdown :as md]
   [nuzzle.util :as util]))

(def config-path "test-resources/edn/config-1.edn")

(def config (conf/load-specified-config config-path {}))

(def site-data-map (util/convert-site-data-to-map (:site-data config)))

(deftest create-render-content-fn
  (let [{:keys [content]} (get site-data-map [:about])
        render-content (md/create-render-content-fn [:about] content nil)]
    (is (fn? render-content))
    (is (= "<h1 id=\"about\">About</h1><p>This is a site for testing the Clojure static site generator called Nuzzle.</p>"
           (str (render-content))))
    (is (= "<p>Foo bar.</p><h2>The story of foo</h2><p>Foo loves bar. But they are thousands of miles apart</p>"
         (str ((md/create-render-content-fn [:foo] "test-resources/html/foo.html" nil)))))))
