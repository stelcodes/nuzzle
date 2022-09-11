(ns nuzzle.schemas-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is]]
   [nuzzle.schemas]))

(deftest author
  (is (s/valid? :nuzzle/author {:name "Stelly Luna"}))
  (is (s/valid? :nuzzle/author {:name "Stelly Luna" :email "stel@email.com"}))
  (is (s/valid? :nuzzle/author {:name "Stelly Luna" :email "stel@email.com" :url "https://stel.com"}))
  (is (not (s/valid? :nuzzle/author {:name 77777777777})))
  (is (not (s/valid? :nuzzle/author {:name "Stelly Luna" :email "stel@email.com" :url "stel.com"})))
  (is (not (s/valid? :nuzzle/author {:name "Stelly Luna" :emailz "stel@email.com"}))))
  (is (not (s/valid? :nuzzle/author {:email "stel@email.com"})))
