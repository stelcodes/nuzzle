(ns nuzzle.hiccup-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.hiccup :as hiccup]))

;; (deftest highlight-code
;;   (let [code "(def foo (let [x (+ 5 7)] (println x)))"]
;;     ;; Chroma 2.2.0
;;     (testing "chroma HTML output"
;;       (is (= "<span class=\"p\">(</span><span class=\"k\">def </span><span class=\"nv\">foo</span> <span class=\"p\">(</span><span class=\"k\">let </span><span class=\"p\">[</span><span class=\"nv\">x</span> <span class=\"p\">(</span><span class=\"nb\">+ </span><span class=\"mi\">5</span> <span class=\"mi\">7</span><span class=\"p\">)]</span> <span class=\"p\">(</span><span class=\"nb\">println </span><span class=\"nv\">x</span><span class=\"p\">)))</span>"
;;              (con/highlight-code code "clojure" {:nuzzle/syntax-highlighter {:provider :chroma}})))
;;       (is (= "(<span style=\"color:#fb660a;font-weight:bold\">def </span><span style=\"color:#fb660a\">foo</span> (<span style=\"color:#fb660a;font-weight:bold\">let </span>[<span style=\"color:#fb660a\">x</span> (+ <span style=\"color:#0086f7;font-weight:bold\">5</span> <span style=\"color:#0086f7;font-weight:bold\">7</span>)] (println <span style=\"color:#fb660a\">x</span>)))"
;;              (con/highlight-code code "clojure" {:nuzzle/syntax-highlighter {:provider :chroma :style "fruity"}})))
;;       (is (= "<span class=\"p\">(</span><span class=\"k\">def </span><span class=\"nv\">foo</span> <span class=\"p\">(</span><span class=\"k\">let </span><span class=\"p\">[</span><span class=\"nv\">x</span> <span class=\"p\">(</span><span class=\"nb\">+ </span><span class=\"mi\">5</span> <span class=\"mi\">7</span><span class=\"p\">)]</span> <span class=\"p\">(</span><span class=\"nb\">println </span><span class=\"nv\">x</span><span class=\"p\">)))</span>"
;;              (con/highlight-code code "clojure" {:nuzzle/syntax-highlighter {:provider :chroma :line-numbers? true}})))
;;       (is (= "(<span style=\"color:#fb660a;font-weight:bold\">def </span><span style=\"color:#fb660a\">foo</span> (<span style=\"color:#fb660a;font-weight:bold\">let </span>[<span style=\"color:#fb660a\">x</span> (+ <span style=\"color:#0086f7;font-weight:bold\">5</span> <span style=\"color:#0086f7;font-weight:bold\">7</span>)] (println <span style=\"color:#fb660a\">x</span>)))"
;;              (con/highlight-code code "clojure" {:nuzzle/syntax-highlighter {:provider :chroma :style "fruity" :line-numbers? true}}))))
;;     ;; Pygmentize 2.12.0
;;     (testing "pygments HTML output"
;;       (is (= "<span class=\"p\">(</span><span class=\"k\">def </span><span class=\"nv\">foo</span><span class=\"w\"> </span><span class=\"p\">(</span><span class=\"k\">let </span><span class=\"p\">[</span><span class=\"nv\">x</span><span class=\"w\"> </span><span class=\"p\">(</span><span class=\"nb\">+ </span><span class=\"mi\">5</span><span class=\"w\"> </span><span class=\"mi\">7</span><span class=\"p\">)]</span><span class=\"w\"> </span><span class=\"p\">(</span><span class=\"nb\">println </span><span class=\"nv\">x</span><span class=\"p\">)))</span><span class=\"w\"></span>\n"
;;              (con/highlight-code code "clojure" {:nuzzle/syntax-highlighter {:provider :pygments}})))
;;
;;       (is (= "<span style=\"color: #ffffff\">(</span><span style=\"color: #fb660a; font-weight: bold\">def </span><span style=\"color: #fb660a\">foo</span><span style=\"color: #888888\"> </span><span style=\"color: #ffffff\">(</span><span style=\"color: #fb660a; font-weight: bold\">let </span><span style=\"color: #ffffff\">[</span><span style=\"color: #fb660a\">x</span><span style=\"color: #888888\"> </span><span style=\"color: #ffffff\">(+ </span><span style=\"color: #0086f7; font-weight: bold\">5</span><span style=\"color: #888888\"> </span><span style=\"color: #0086f7; font-weight: bold\">7</span><span style=\"color: #ffffff\">)]</span><span style=\"color: #888888\"> </span><span style=\"color: #ffffff\">(println </span><span style=\"color: #fb660a\">x</span><span style=\"color: #ffffff\">)))</span><span style=\"color: #888888\"></span>\n"
;;              (con/highlight-code code "clojure" {:nuzzle/syntax-highlighter {:provider :pygments :style "fruity"}})))
;;
;;       (is (= (con/highlight-code "(def foo (let [x (+ 5 7)] (println x)))" "clojure" {:nuzzle/syntax-highlighter {:provider :pygments :line-numbers? true}})
;;              (con/highlight-code code "clojure" {:nuzzle/syntax-highlighter {:provider :pygments :line-numbers? true}}))))))

(deftest transform-hiccup
  (let [hiccup-with-custom-element [:div {:class "hi"} [:youtube {:title "some title" :id "12345"}]]
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
    (is (= (hiccup/transform-hiccup (list [:div] hiccup-with-custom-element) {:youtube hiccup/youtube})
           (list
            [:div]
            expected-result)))
    (is (= (hiccup/transform-hiccup hiccup-with-custom-element {:youtube hiccup/youtube})
           expected-result))))
