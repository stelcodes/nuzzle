(ns nuzzle.pages-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.pages :as pages]
   [nuzzle.test-util :as test-util]))

(deftest remove-draft-pages
  (let [x {[:foo :bar]
           {:nuzzle/draft? true}}]
    (is (= {} (pages/remove-draft-pages x)))))

(comment (remove-draft-pages))

(deftest add-tag-pages
  (is (= {[:blog :foo]
          {:nuzzle/tags #{:bar :baz}}
          [:about] {}
          [:tags :bar]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/render-page test-util/render-page
           :nuzzle/title "Tag bar"}
          [:tags :baz]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/render-page test-util/render-page
           :nuzzle/title "Tag baz"}}
         (pages/add-tag-pages {[:blog :foo] {:nuzzle/tags #{:bar :baz}} [:about] {}} :render-page test-util/render-page)))
  (is (= (merge
          test-util/twin-peaks-pages
          {[:tags :nuzzle]
          {:nuzzle/index #{[:blog :nuzzle-rocks] [:blog :why-nuzzle]},
           :nuzzle/render-page test-util/render-page
           :nuzzle/title "Tag nuzzle"}
          [:tags :colors]
          {:nuzzle/index #{[:blog :favorite-color]},
           :nuzzle/render-page test-util/render-page
           :nuzzle/title "Tag colors"}})
         (pages/add-tag-pages test-util/twin-peaks-pages :render-page test-util/render-page))))

(deftest create-get-pages
  (let [get-pages (-> test-util/twin-peaks-pages pages/load-pages pages/create-get-pages)]
    (is (= "About" (:nuzzle/title (get-pages [:about]))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"get-pages error"
                          (get-pages :bad-key)))))
