(ns nuzzle.generator-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [nuzzle.config :as conf]
            [nuzzle.util :as util]
            [nuzzle.generator :as gen]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/read-config-path config-path))

(deftest create-tag-index
  (is (= {[:tags :bar]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/title "#bar"}
          [:tags :baz]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/title "#baz"}}
         (gen/create-tag-index {[:blog :foo] {:nuzzle/tags #{:bar :baz}} [:about] {}})))
  (is (= {[:tags :nuzzle]
          {:nuzzle/index #{[:blog :nuzzle-rocks] [:blog :why-nuzzle]},
           :nuzzle/title "#nuzzle"}
          [:tags :colors]
          {:nuzzle/index #{[:blog :favorite-color]},
           :nuzzle/title "#colors"}}
         (gen/create-tag-index (config)))))

(deftest create-group-index
  (is (= {[:blog-posts]
          {:nuzzle/index #{[:blog-posts :foo] [:blog-posts :archive]}, :nuzzle/title "Blog Posts"},
          [:blog-posts :archive]
          {:nuzzle/index #{[:blog-posts :archive :baz]},
           :nuzzle/title "Archive"}
          [:projects]
          {:nuzzle/index #{[:projects :bee]}, :nuzzle/title "Projects"}
          []
          {:nuzzle/index #{[:blog-posts] [:projects]}
           :nuzzle/title "Home"}}
         (gen/create-group-index {[:blog-posts :foo] {:nuzzle/title "Foo"}
                                  [:blog-posts :archive :baz] {:nuzzle/title "Baz"}
                                  [:projects :bee] {:nuzzle/title "Bee"}})))
  (is (= {[:blog]
          {:nuzzle/index
           #{[:blog :why-nuzzle] [:blog :favorite-color] [:blog :nuzzle-rocks]},
           :nuzzle/title "Blog"}
          []
          {:nuzzle/index #{[:about] [:blog]}
           :nuzzle/title "Home"}}
         (gen/create-group-index (config)))))

(deftest page-key->url
  (is (= "/blog-posts/my-hobbies/" (util/page-key->url [:blog-posts :my-hobbies])))
  (is (= "/about/" (util/page-key->url [:about]))))

(comment (run-tests))
