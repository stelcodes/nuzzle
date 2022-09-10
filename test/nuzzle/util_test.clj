(ns nuzzle.util-test
  (:require
   [clojure.test :refer [deftest is]]
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
         (util/find-hiccup-str #"foo" [:div "bar" [:div {:class "foobaz"} [:p "foobar"]]]))))
