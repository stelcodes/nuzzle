(ns nuzzle.markdown-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.markdown :as md]
   [nuzzle.util :as util]))

(def config-path "test-resources/edn/config-1.edn")

(def config (conf/load-specified-config config-path {}))

(def site-data-map (util/convert-site-data-to-map (:site-data config)))

(deftest highlight-code
  (let [code "(def foo (let [x (+ 5 7)] (println x)))"
        result (md/highlight-code "fruity" "clojure" code)]
    (is (= "(<span style=\"color:#fb660a;font-weight:bold\">def </span><span style=\"color:#fb660a\">foo</span> (<span style=\"color:#fb660a;font-weight:bold\">let </span>[<span style=\"color:#fb660a\">x</span> (+ <span style=\"color:#0086f7;font-weight:bold\">5</span> <span style=\"color:#0086f7;font-weight:bold\">7</span>)] (println <span style=\"color:#fb660a\">x</span>)))"
         result))))

(deftest create-render-markdown-fn
  (let [{:keys [markdown]} (get site-data-map [:about])
        render-markdown (md/create-render-markdown-fn [:about] markdown nil)]
    (is (fn? render-markdown))
    (is (= "<h1 id=\"about\">About</h1><p>This is a site for testing the Clojure static site generator called Nuzzle.</p>"
           (str (render-markdown))))
    (is (= "<p>Foo bar.</p><h2>The story of foo</h2><p>Foo loves bar. But they are thousands of miles apart</p>"
         (str ((md/create-render-markdown-fn [:foo] "test-resources/html/foo.html" nil)))))))
