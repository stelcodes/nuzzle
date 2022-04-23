(ns nuzzle.generator-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [nuzzle.util :as util]
            [nuzzle.generator :as gen]))

(def config-path "test-resources/edn/config-1.edn")

(defn render-webpage [_] (constantly [:h1 "test"]))

(def config (gen/load-config config-path {}))

(def site-data-map (gen/convert-site-data-to-map (:site-data config)))

(deftest load-site-data
  (is (= (gen/load-config config-path {})
         {:output-dir "/tmp/nuzzle-test-out",
          :dev-port 6899,
          :remove-drafts? false,
          :render-webpage render-webpage,
          :rss-opts
          {:title "Foo's blog",
           :description "Rants about foo and thoughts about bar",
           :link "https://foobar.com",
           :author "foo@bar.com (Foo Bar)"},
          :static-dir "public",
          :site-data
          [{:id []}
           {:id [:blog :nuzzle-rocks],
            :title "10 Reasons Why Nuzzle Rocks",
            :content "test-resources/markdown/nuzzle-rocks.md",
            :rss true,
            :tags [:nuzzle]}
           {:id [:blog :why-nuzzle],
            :title "Why I Made Nuzzle",
            :content "test-resources/markdown/why-nuzzle.md",
            :rss true,
            :tags [:nuzzle]}
           {:id [:blog :favorite-color],
            :title "What's My Favorite Color? It May Suprise You.",
            :content "test-resources/markdown/favorite-color.md",
            :rss true,
            :tags [:colors]}
           {:id [:about],
            :title "About",
            :content "test-resources/markdown/about.md"}
           {:id :meta, :twitter "https://twitter/foobar"}]})))

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

(deftest create-render-content-fn
  (let [{:keys [content]} (get site-data-map [:about])
        render-content (gen/create-render-content-fn [:about] content nil)]
    (is (fn? render-content))
    (is (= "<h1 id=\"about\">About</h1><p>This is a site for testing the Clojure static site generator called Nuzzle.</p>"
           (str (render-content))))
    (is (= "<p>Foo bar.</p><h2>The story of foo</h2><p>Foo loves bar. But they are thousands of miles apart</p>"
         (str ((gen/create-render-content-fn [:foo] "test-resources/html/foo.html" nil)))))))

(deftest realize-pages
  (let [realized-pages (gen/realize-pages site-data-map config)
        without-render-content (reduce-kv #(assoc %1 %2 (dissoc %3 :render-content))
                                          {}
                                          realized-pages)]
    (doseq [[id info] realized-pages
            :when (vector? id)]
      (is (fn? (:render-content info))))
    (is (= {[]
            {:uri "/"}
            [:blog :nuzzle-rocks]
            {:title "10 Reasons Why Nuzzle Rocks",
             :content "test-resources/markdown/nuzzle-rocks.md",
             :rss true
             :tags [:nuzzle],
             :uri "/blog/nuzzle-rocks/"}
            [:blog :why-nuzzle]
            {:title "Why I Made Nuzzle",
             :content "test-resources/markdown/why-nuzzle.md",
             :rss true
             :tags [:nuzzle],
             :uri "/blog/why-nuzzle/"}
            [:blog :favorite-color]
            {:title "What's My Favorite Color? It May Suprise You.",
             :content "test-resources/markdown/favorite-color.md",
             :rss true
             :tags [:colors],
             :uri "/blog/favorite-color/"}
            [:about]
            {:title "About",
             :content "test-resources/markdown/about.md",
             :uri "/about/"}
            :meta
            {:twitter "https://twitter/foobar"}}
           without-render-content))))

(deftest id->uri
  (is (= "/blog-posts/my-hobbies/" (util/id->uri [:blog-posts :my-hobbies])))
  (is (= "/about/" (util/id->uri [:about]))))

(deftest create-rss-feed
  (let [realized-site-data (gen/realize-site-data config)]
    (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"><channel><atom:link href=\"https://foobar.com\" rel=\"self\" type=\"application/rss+xml\"/><title>Foo's blog</title><description>Rants about foo and thoughts about bar</description><link>https://foobar.com</link><generator>clj-rss</generator><item><title>Why I Made Nuzzle</title><guid isPermaLink=\"false\">https://foobar.com/blog/why-nuzzle/</guid><author>foo@bar.com (Foo Bar)</author></item><item><title>What's My Favorite Color? It May Suprise You.</title><guid isPermaLink=\"false\">https://foobar.com/blog/favorite-color/</guid><author>foo@bar.com (Foo Bar)</author></item><item><title>10 Reasons Why Nuzzle Rocks</title><guid isPermaLink=\"false\">https://foobar.com/blog/nuzzle-rocks/</guid><author>foo@bar.com (Foo Bar)</author></item></channel></rss>"
           (gen/create-rss-feed
            realized-site-data (:rss-opts config))))))

#_
(deftest realize-site-data
  (is (= (gen/realize-site-data (:site-data nuzzle-config) (:remove-drafts? nuzzle-config)))))

#_
(deftest export
  (let [y {[:about] {:title "About"}}
        x {:config y :include-drafts? true :render-webpage (constantly "<h1>Test</h1>") :output-dir "/tmp/out"}]
    (generator/export x)))

(comment (run-tests))
