(ns nuzzle.generator-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [nuzzle.config :as conf]
            [nuzzle.util :as util]
            [nuzzle.generator :as gen]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/read-config-path config-path))

(defn site-data [] (:site-data (config)))

(defn site-data-map [] (util/convert-site-data-to-map (site-data)))

(deftest create-tag-index
  (is (= {[:tags :bar]
          {:index #{[:blog :foo]},
           :title "#bar"}
          [:tags :baz]
          {:index #{[:blog :foo]},
           :title "#baz"}}
         (gen/create-tag-index {[:blog :foo] {:tags #{:bar :baz}} [:about] {} }) ))
  (is (= {[:tags :nuzzle]
          {:index #{[:blog :nuzzle-rocks] [:blog :why-nuzzle]},
           :title "#nuzzle"}
          [:tags :colors]
          {:index #{[:blog :favorite-color]},
           :title "#colors"}}
         (gen/create-tag-index (site-data-map)))))

(deftest create-group-index
  (is (= {[:blog-posts]
          {:index #{[:blog-posts :foo] [:blog-posts :archive]}, :title "Blog Posts"},
          [:blog-posts :archive]
          {:index #{[:blog-posts :archive :baz]},
           :title "Archive"}
          [:projects]
          {:index #{[:projects :bee]}, :title "Projects"}
          []
          {:index #{[:blog-posts] [:projects]}}}
         (gen/create-group-index {[:blog-posts :foo] {:title "Foo"}
                                  [:blog-posts :archive :baz] {:title "Baz"}
                                  [:projects :bee] {:title "Bee"}})))
  (is (= {[:blog]
          {:index
           #{[:blog :why-nuzzle] [:blog :favorite-color] [:blog :nuzzle-rocks]},
           :title "Blog"}
          []
          {:index #{[:about] [:blog]}}}
         (gen/create-group-index (site-data-map)))))

(deftest realize-pages
  (let [mod-site-data (-> (config)
                          (update :site-data util/convert-site-data-to-map)
                          (gen/realize-pages)
                          (:site-data))
        without-render-content (reduce-kv #(assoc %1 %2 (dissoc %3 :nuzzle/render-content))
                                          {}
                                          mod-site-data)]
    (doseq [[id info] mod-site-data
            :when (vector? id)]
      (is (fn? (:nuzzle/render-content info))))
    (is (= {[]
            {:nuzzle/url "/"}
            [:blog :nuzzle-rocks]
            {:title "10 Reasons Why Nuzzle Rocks",
             :content "test-resources/markdown/nuzzle-rocks.md",
             :modified "2022-05-09"
             :rss? true
             :tags #{:nuzzle},
             :nuzzle/url "/blog/nuzzle-rocks/"}
            [:blog :why-nuzzle]
            {:title "Why I Made Nuzzle",
             :content "test-resources/markdown/why-nuzzle.md",
             :rss? true
             :tags #{:nuzzle},
             :nuzzle/url "/blog/why-nuzzle/"}
            [:blog :favorite-color]
            {:title "What's My Favorite Color? It May Suprise You.",
             :content "test-resources/markdown/favorite-color.md",
             :rss? true
             :tags #{:colors},
             :nuzzle/url "/blog/favorite-color/"}
            [:about]
            {:title "About",
             :content "test-resources/markdown/about.md",
             :nuzzle/url "/about/"}
            :meta
            {:twitter "https://twitter/foobar"}}
           without-render-content))))

(deftest id->url
  (is (= "/blog-posts/my-hobbies/" (util/id->url [:blog-posts :my-hobbies])))
  (is (= "/about/" (util/id->url [:about]))))

(comment (run-tests))
