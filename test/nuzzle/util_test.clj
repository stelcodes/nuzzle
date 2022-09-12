(ns nuzzle.util-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [nuzzle.util :as util]))

(deftest vectorize-url
  (is (= (list [:about] [] [:blog-posts :foo])
         (map util/vectorize-url ["/about/" "/" "/blog-posts/foo/"]))))

(deftest stringify-url
  (is (= "/blog-posts/my-hobbies/" (util/stringify-url [:blog-posts :my-hobbies])))
  (is (= "/about/" (util/stringify-url [:about]))))

(deftest time-str->?inst
  (is (inst? (util/time-str->?inst "2022-05-04")))
  (is (inst? (util/time-str->?inst "2022-05-04T05:04")))
  (is (inst? (util/time-str->?inst "2022-05-04T05:04:03")))
  (is (inst? (util/time-str->?inst "2022-05-04T05:04:03Z")))
  (is (nil? (util/time-str->?inst "2022-05-32")))
  (is (nil? (util/time-str->?inst "2022-05-04T05:0")))
  (is (nil? (util/time-str->?inst "2022-05-04T05:72")))
  (is (nil? (util/time-str->?inst "2022-05-04T05:04:039Z"))))

(deftest find-hiccup-str
  (is (= "foobar"
         (util/find-hiccup-str [:div "bar" [:div {:class "foobaz"} [:p "foobar"]]] #"foo"))))

(deftest generate-highlight-commands
  (testing "generating chroma command"
    (is (= (list "chroma" "--lexer=clojure" "--formatter=html" "--html-only" "--html-prevent-surrounding-pre" "/tmp/foo.clj")
           (util/generate-chroma-command "/tmp/foo.clj" "clojure")))
    (is (= (list "chroma" "--lexer=clojure" "--formatter=html" "--html-only" "--html-prevent-surrounding-pre" "--html-lines" "/tmp/foo.clj")
           (util/generate-chroma-command "/tmp/foo.clj" "clojure" {:line-numbers? true})))
    (is (= (list "chroma" "--lexer=clojure" "--formatter=html" "--html-only" "--html-prevent-surrounding-pre" "--html-inline-styles" "--style=algol_nu" "/tmp/foo.clj")
           (util/generate-chroma-command "/tmp/foo.clj" "clojure" {:style "algol_nu"})))
    (is (= (list "chroma" "--lexer=clojure" "--formatter=html" "--html-only" "--html-prevent-surrounding-pre" "--html-inline-styles" "--style=algol_nu" "--html-lines" "/tmp/foo.clj")
           (util/generate-chroma-command "/tmp/foo.clj" "clojure" {:style "algol_nu" :line-numbers? true}))))
  (testing "generating pygments command"
    (is (= (list "pygmentize" "-f" "html" "-l" "clojure" "-O" "nowrap" "/tmp/foo.clj")
           (util/generate-pygments-command "/tmp/foo.clj" "clojure")))
    (is (= (list "pygmentize" "-f" "html" "-l" "clojure" "-O" "linenos=inline"  "/tmp/foo.clj")
           (util/generate-pygments-command "/tmp/foo.clj" "clojure" {:line-numbers? true})))
    (is (= (list "pygmentize" "-f" "html" "-l" "clojure" "-O" "nowrap,noclasses,style=algol_nu" "/tmp/foo.clj")
           (util/generate-pygments-command "/tmp/foo.clj" "clojure" {:style "algol_nu"})))
    (is (= (list "pygmentize" "-f" "html" "-l" "clojure" "-O" "noclasses,style=algol_nu,linenos=inline" "/tmp/foo.clj")
           (util/generate-pygments-command "/tmp/foo.clj" "clojure" {:style "algol_nu" :line-numbers? true})))))

