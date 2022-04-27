(ns nuzzle.markdown-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.markdown :as md]))

(def config-path "test-resources/edn/config-1.edn")

(def config (conf/load-specified-config config-path {}))

(deftest highlight-code
  (let [code "(def foo (let [x (+ 5 7)] (println x)))"
        result (md/highlight-code "fruity" "clojure" code)]
    (is (= "(<span style=\"color:#fb660a;font-weight:bold\">def </span><span style=\"color:#fb660a\">foo</span> (<span style=\"color:#fb660a;font-weight:bold\">let </span>[<span style=\"color:#fb660a\">x</span> (+ <span style=\"color:#0086f7;font-weight:bold\">5</span> <span style=\"color:#0086f7;font-weight:bold\">7</span>)] (println <span style=\"color:#fb660a\">x</span>)))"
         result))))

(deftest create-render-markdown-fn
  (let [{:keys [markdown]} (get-in config [:site-data [:about]])
        render-markdown (md/create-render-markdown-fn [:about] markdown nil)]
    (is (fn? render-markdown))
    (is (= (list [:h1 {:id "about"} "About"] [:p {} "This is a site for testing the Clojure static site generator called Nuzzle."])
           (render-markdown)))))
