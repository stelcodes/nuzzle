(ns nuzzle.api-test
  (:require
   [clojure.test :as t]
   [nuzzle.api :as api]
   [nuzzle.test-util :as test-util]))

(t/deftest add-tag-index-pages
  (t/is (= {[:blog :foo]
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
         (api/add-tag-pages {[:blog :foo] {:nuzzle/tags #{:bar :baz}} [:about] {}} test-util/render-page)))
  (t/is (= (merge
          test-util/twin-peaks-pages
          {[:tags :nuzzle]
          {:nuzzle/index #{[:blog :nuzzle-rocks] [:blog :why-nuzzle]},
           :nuzzle/render-page test-util/render-page
           :nuzzle/title "Tag nuzzle"}
          [:tags :colors]
          {:nuzzle/index #{[:blog :favorite-color]},
           :nuzzle/render-page test-util/render-page
           :nuzzle/title "Tag colors"}})
         (api/add-tag-pages test-util/twin-peaks-pages test-util/render-page))))
