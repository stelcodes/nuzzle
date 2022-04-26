(ns nuzzle.generator-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [nuzzle.config :as conf]
            [nuzzle.util :as util]
            [nuzzle.generator :as gen]))

(def config-path "test-resources/edn/config-1.edn")

(def config (conf/load-specified-config config-path {}))

(def site-data-map (util/convert-site-data-to-map (:site-data config)))

(deftest create-tag-index
  (is (= {[:tags :bar]
          {:index [[:blog :foo]],
           :title "#bar",
           :uri "/tags/bar/"},
          [:tags :baz]
          {:index [[:blog :foo]],
           :title "#baz",
           :uri "/tags/baz/"}}
         (gen/create-tag-index {[:blog :foo] {:tags [:bar :baz]} [:about] {} }) ))
  (is (= {[:tags :nuzzle]
          {:index [[:blog :nuzzle-rocks] [:blog :why-nuzzle]],
           :title "#nuzzle",
           :uri "/tags/nuzzle/"},
          [:tags :colors]
          {:index [[:blog :favorite-color]],
           :title "#colors",
           :uri "/tags/colors/"}}
         (gen/create-tag-index site-data-map))))

(deftest create-group-index
  (is (= {[:blog-posts]
          {:index [[:blog-posts :foo]], :title "Blog Posts", :uri "/blog-posts/"},
          [:blog-posts :archive]
          {:index [[:blog-posts :archive :baz]],
           :title "Archive",
           :uri "/blog-posts/archive/"},
          [:projects]
          {:index [[:projects :bee]], :title "Projects", :uri "/projects/"}}
         (gen/create-group-index {[:blog-posts :foo] {:title "Foo"} [:blog-posts :archive :baz] {:title "Baz"} [:projects :bee] {:title "Bee"}})))
  (is (= {[:blog]
          {:index
           [[:blog :nuzzle-rocks] [:blog :why-nuzzle] [:blog :favorite-color]],
           :title "Blog",
           :uri "/blog/"}}
         (gen/create-group-index site-data-map))))

(deftest realize-pages
  (let [realized-pages (gen/realize-pages site-data-map config)
        without-render-markdown (reduce-kv #(assoc %1 %2 (dissoc %3 :render-markdown))
                                          {}
                                          realized-pages)]
    (doseq [[id info] realized-pages
            :when (vector? id)]
      (is (fn? (:render-markdown info))))
    (is (= {[]
            {:uri "/"}
            [:blog :nuzzle-rocks]
            {:title "10 Reasons Why Nuzzle Rocks",
             :markdown "test-resources/markdown/nuzzle-rocks.md",
             :rss? true
             :tags [:nuzzle],
             :uri "/blog/nuzzle-rocks/"}
            [:blog :why-nuzzle]
            {:title "Why I Made Nuzzle",
             :markdown "test-resources/markdown/why-nuzzle.md",
             :rss? true
             :tags [:nuzzle],
             :uri "/blog/why-nuzzle/"}
            [:blog :favorite-color]
            {:title "What's My Favorite Color? It May Suprise You.",
             :markdown "test-resources/markdown/favorite-color.md",
             :rss? true
             :tags [:colors],
             :uri "/blog/favorite-color/"}
            [:about]
            {:title "About",
             :markdown "test-resources/markdown/about.md",
             :uri "/about/"}
            :meta
            {:twitter "https://twitter/foobar"}}
           without-render-markdown))))

(deftest id->uri
  (is (= "/blog-posts/my-hobbies/" (util/id->uri [:blog-posts :my-hobbies])))
  (is (= "/about/" (util/id->uri [:about]))))

#_
(deftest realize-site-data
  (is (= (gen/realize-site-data (:site-data nuzzle-config) (:remove-drafts? nuzzle-config)))))

#_
(deftest export
  (let [y {[:about] {:title "About"}}
        x {:config y :include-drafts? true :render-webpage (constantly "<h1>Test</h1>") :output-dir "/tmp/out"}]
    (generator/export x)))

(comment (run-tests))
