(ns nuzzle.schemas-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is]]
   [nuzzle.schemas]))

(deftest author-registry
  (is (s/valid? :nuzzle/author-registry {:stel {:name "Stelly Luna"}}))
  (is (s/valid? :nuzzle/author-registry {:stel {:name "Stelly Luna" :email "stel@email.com"}}))
  (is (s/valid? :nuzzle/author-registry {:stel {:name "Stelly Luna" :email "stel@email.com" :url "https://stel.com"}}))
  (is (not (s/valid? :nuzzle/author-registry {:stel {:name 77777777777}})))
  (is (not (s/valid? :nuzzle/author-registry {:stel {:name "Stelly Luna" :email "stel@email.com" :url "stel.com"}})))
  (is (not (s/valid? :nuzzle/author-registry {:stel {:name "Stelly Luna" :emailz "stel@email.com"}}))))
  (is (not (s/valid? :nuzzle/author-registry {:stel {:email "stel@email.com"}})))
