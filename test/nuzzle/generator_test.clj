(ns nuzzle.generator-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [nuzzle.config :as conf]
            [nuzzle.util :as util]
            [nuzzle.generator :as gen]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/read-config-path config-path))

(deftest create-tag-index
  (is (= {[:tags :bar]
          {:index #{[:blog :foo]},
           :title "#bar"}
          [:tags :baz]
          {:index #{[:blog :foo]},
           :title "#baz"}}
         (gen/create-tag-index {[:blog :foo] {:tags #{:bar :baz}} [:about] {}})))
  (is (= {[:tags :nuzzle]
          {:index #{[:blog :nuzzle-rocks] [:blog :why-nuzzle]},
           :title "#nuzzle"}
          [:tags :colors]
          {:index #{[:blog :favorite-color]},
           :title "#colors"}}
         (gen/create-tag-index (config)))))

(deftest create-group-index
  (is (= {[:blog-posts]
          {:index #{[:blog-posts :foo] [:blog-posts :archive]}, :title "Blog Posts"},
          [:blog-posts :archive]
          {:index #{[:blog-posts :archive :baz]},
           :title "Archive"}
          [:projects]
          {:index #{[:projects :bee]}, :title "Projects"}
          []
          {:index #{[:blog-posts] [:projects]}
           :title "Home"}}
         (gen/create-group-index {[:blog-posts :foo] {:title "Foo"}
                                  [:blog-posts :archive :baz] {:title "Baz"}
                                  [:projects :bee] {:title "Bee"}})))
  (is (= {[:blog]
          {:index
           #{[:blog :why-nuzzle] [:blog :favorite-color] [:blog :nuzzle-rocks]},
           :title "Blog"}
          []
          {:index #{[:about] [:blog]}
           :title "Home"}}
         (gen/create-group-index (config)))))

(deftest id->url
  (is (= "/blog-posts/my-hobbies/" (util/id->url [:blog-posts :my-hobbies])))
  (is (= "/about/" (util/id->url [:about]))))

(comment (run-tests))
