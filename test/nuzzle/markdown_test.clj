(ns nuzzle.markdown-test
  (:require
   [clojure.test :refer [deftest is run-tests]]
   [nuzzle.config :as conf]
   [nuzzle.markdown :as md]))

(def config-path "test-resources/edn/config-1.edn")

(def config (conf/load-specified-config config-path {}))

(deftest highlight-code
  (let [code "(def foo (let [x (+ 5 7)] (println x)))"
        chroma-fruity {:markdown-opts {:syntax-highlighting {:provider :chroma :style "fruity"}}}
        chroma-no-style {:markdown-opts {:syntax-highlighting {:provider :chroma}}}
        pygments-fruity {:markdown-opts {:syntax-highlighting {:provider :pygments :style "fruity"}}}
        pygments-no-style {:markdown-opts {:syntax-highlighting {:provider :pygments}}}]
    ;; Chroma 2.0.0-alpha4
    (is (= "<span style=\"display:flex;\"><span>(<span style=\"color:#fb660a;font-weight:bold\">def </span><span style=\"color:#fb660a\">foo</span> (<span style=\"color:#fb660a;font-weight:bold\">let </span>[<span style=\"color:#fb660a\">x</span> (+ <span style=\"color:#0086f7;font-weight:bold\">5</span> <span style=\"color:#0086f7;font-weight:bold\">7</span>)] (println <span style=\"color:#fb660a\">x</span>)))</span></span>"
           (md/highlight-code code "clojure" chroma-fruity)))
    (is (= "<span class=\"line\"><span class=\"cl\"><span class=\"p\">(</span><span class=\"k\">def </span><span class=\"nv\">foo</span> <span class=\"p\">(</span><span class=\"k\">let </span><span class=\"p\">[</span><span class=\"nv\">x</span> <span class=\"p\">(</span><span class=\"nb\">+ </span><span class=\"mi\">5</span> <span class=\"mi\">7</span><span class=\"p\">)]</span> <span class=\"p\">(</span><span class=\"nb\">println </span><span class=\"nv\">x</span><span class=\"p\">)))</span></span></span>"
           (md/highlight-code code "clojure" chroma-no-style)))
    ;; Pygmentize 2.12.0
    (is (= "<span style=\"color: #ffffff\">(</span><span style=\"color: #fb660a; font-weight: bold\">def </span><span style=\"color: #fb660a\">foo</span><span style=\"color: #888888\"> </span><span style=\"color: #ffffff\">(</span><span style=\"color: #fb660a; font-weight: bold\">let </span><span style=\"color: #ffffff\">[</span><span style=\"color: #fb660a\">x</span><span style=\"color: #888888\"> </span><span style=\"color: #ffffff\">(+ </span><span style=\"color: #0086f7; font-weight: bold\">5</span><span style=\"color: #888888\"> </span><span style=\"color: #0086f7; font-weight: bold\">7</span><span style=\"color: #ffffff\">)]</span><span style=\"color: #888888\"> </span><span style=\"color: #ffffff\">(println </span><span style=\"color: #fb660a\">x</span><span style=\"color: #ffffff\">)))</span><span style=\"color: #888888\"></span>\n"
           (md/highlight-code code "clojure" pygments-fruity)))
    (is (= "<span class=\"p\">(</span><span class=\"k\">def </span><span class=\"nv\">foo</span><span class=\"w\"> </span><span class=\"p\">(</span><span class=\"k\">let </span><span class=\"p\">[</span><span class=\"nv\">x</span><span class=\"w\"> </span><span class=\"p\">(</span><span class=\"nb\">+ </span><span class=\"mi\">5</span><span class=\"w\"> </span><span class=\"mi\">7</span><span class=\"p\">)]</span><span class=\"w\"> </span><span class=\"p\">(</span><span class=\"nb\">println </span><span class=\"nv\">x</span><span class=\"p\">)))</span><span class=\"w\"></span>\n"
           (md/highlight-code code "clojure" pygments-no-style)))))

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
