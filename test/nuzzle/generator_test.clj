(ns nuzzle.generator-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [nuzzle.config :as conf]
            [nuzzle.generator :as gen]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/read-config-from-path config-path))

(deftest create-tag-index-page-entries
  (is (= {[:tags :bar]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/title "#bar"}
          [:tags :baz]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/title "#baz"}}
         (gen/create-tag-index-page-entries {[:blog :foo] {:nuzzle/tags #{:bar :baz}} [:about] {}})))
  (is (= {[:tags :nuzzle]
          {:nuzzle/index #{[:blog :nuzzle-rocks] [:blog :why-nuzzle]},
           :nuzzle/title "#nuzzle"}
          [:tags :colors]
          {:nuzzle/index #{[:blog :favorite-color]},
           :nuzzle/title "#colors"}}
         (gen/create-tag-index-page-entries (config)))))

(deftest create-hierarchical-index-page-entries
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
         (gen/create-hierarchical-index-page-entries {[:blog-posts :foo] {:nuzzle/title "Foo"}
                                  [:blog-posts :archive :baz] {:nuzzle/title "Baz"}
                                  [:projects :bee] {:nuzzle/title "Bee"}})))
  (is (= {[:blog]
          {:nuzzle/index
           #{[:blog :why-nuzzle] [:blog :favorite-color] [:blog :nuzzle-rocks]},
           :nuzzle/title "Blog"}
          []
          {:nuzzle/index #{[:about] [:blog]}
           :nuzzle/title "Home"}}
         (gen/create-hierarchical-index-page-entries (config)))))

(deftest gen-get-config
  (let [config (conf/load-config-from-path config-path)
        get-config (gen/gen-get-config config)]
    (is (= "https://foobar.com" (get-config :nuzzle/base-url)))
    (is (= "https://twitter/foobar" (get-config :meta :twitter)))
    (is (= "/about/" (get-config [:about] :nuzzle/url)))
    (is (= #{[:blog :favorite-color] [:blog :nuzzle-rocks] [:blog :why-nuzzle]}
           (get-config [:blog] :nuzzle/index)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"get-config error: config key sequence \[:bad-key\] cannot be resolved"
                          (get-config :bad-key)))))

(comment (run-tests))
