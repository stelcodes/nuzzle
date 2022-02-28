(ns codes.stel.nuzzle.generator-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [codes.stel.nuzzle.util :as util]
            [codes.stel.nuzzle.generator :as gen]))

(def site-config {[:blog :nuzzle-rocks]
                  {:title "10 Reasons Why Nuzzle Rocks"
                   :content "markdown/nuzzle-rocks.md"
                   :tags [:nuzzle]}

                  [:blog :why-nuzzle]
                  {:title "Why I Made Nuzzle"
                   :content "markdown/why-nuzzle.md"
                   :tags [:nuzzle]}

                  [:blog :favorite-color]
                  {:title "What's My Favorite Color? It May Suprise You."
                   :content "markdown/favorite-color.md"
                   :tags [:colors]}

                  [:about]
                  {:title "About"
                   :content "markdown/about.md"}

                  :meta
                  {:twitter "https://twitter/foobar"}})

(def global-config {:site-config site-config
                    :remove-drafts? false
                    :render-page (constantly [:h1 "Test"])
                    :static-dir "public"
                    :target-dir "/tmp/nuzzle-test-dist"})

(deftest load-site-config
  (is (= global-config
         (gen/load-site-config global-config))))

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
         (gen/create-tag-index site-config))))

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
         (gen/create-group-index site-config))))

(deftest create-render-content-fn
  (let [{:keys [content]} (get site-config [:about])
        render-content (gen/create-render-content-fn [:about] content)]
    (is (fn? render-content))
    (is (= "<h1>About</h1><p>This is a site for testing the Clojure static site generator called Nuzzle.</p>"
           (str (render-content))))
    (is (= "<p>Foo bar.</p><h2>The story of foo</h2><p>Foo loves bar. But they are thousands of miles apart</p>"
         (str ((gen/create-render-content-fn [:foo] "html/foo.html")))))))

(deftest realize-pages
  (let [realized-pages (gen/realize-pages site-config)
        without-render-content (reduce-kv #(assoc %1 %2 (dissoc %3 :render-content))
                                          {}
                                          realized-pages)]
    (doseq [[id info] realized-pages
            :when (vector? id)]
      (is (fn? (:render-content info))))
    (is (= {[:blog :nuzzle-rocks]
          {:title "10 Reasons Why Nuzzle Rocks",
           :content "markdown/nuzzle-rocks.md",
           :tags [:nuzzle],
           :uri "/blog/nuzzle-rocks/"}
          [:blog :why-nuzzle]
          {:title "Why I Made Nuzzle",
           :content "markdown/why-nuzzle.md",
           :tags [:nuzzle],
           :uri "/blog/why-nuzzle/"}
          [:blog :favorite-color]
          {:title "What's My Favorite Color? It May Suprise You.",
           :content "markdown/favorite-color.md",
           :tags [:colors],
           :uri "/blog/favorite-color/"}
          [:about]
          {:title "About",
           :content "markdown/about.md",
           :uri "/about/"}
          :meta
          {:twitter "https://twitter/foobar"}}
         without-render-content))))

(deftest id->uri
  (is (= "/blog-posts/my-hobbies/" (util/id->uri [:blog-posts :my-hobbies])))
  (is (= "/about/" (util/id->uri [:about]))))

#_
(deftest realize-site-config
  (is (= (gen/realize-site-config (:site-config global-config) (:remove-drafts? global-config)))))

#_
(deftest export
  (let [y {[:about] {:title "About"}}
        x {:config y :include-drafts? true :render-page (constantly "<h1>Test</h1>") :target-dir "/tmp/dist"}]
    (generator/export x)))

(comment (run-tests))
