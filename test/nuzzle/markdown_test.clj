(ns nuzzle.markdown-test
  (:require
   [clojure.test :refer [deftest is run-tests]]
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

(deftest walk-hiccup-for-shortcodes
  (let [hiccup-with-shortcode [:div {:class "hi"} [:youtube {:title "some title" :id "12345"}]]
        expected-result
        [:div {:class "hi"}
         [:div
          {:style
           "position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden;"}
          [:iframe
           {:src "https://www.youtube.com/embed/12345",
            :style
            "position: absolute; top: 0; left: 0; width: 100%; height: 100%; border:0;",
            :title "some title",
            :allowfullscreen true}]]]]
    (is (= (md/walk-hiccup-for-shortcodes (list [:div] hiccup-with-shortcode))
           (list
            [:div]
            expected-result)))
    (is (= (md/walk-hiccup-for-shortcodes hiccup-with-shortcode)
           expected-result))))

(comment ((md/create-render-markdown-fn [:inline-html] "test-resources/markdown/inline-html.md" nil))
         (run-tests))
