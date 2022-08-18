(ns nuzzle.config-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [nuzzle.config :as conf]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/read-config-from-path config-path))

(def config-2-path "test-resources/edn/config-2-bad.edn")

(def render-page (constantly [:h1 "test"]))

(deftest read-config-from-path
  (is (= (conf/read-config-from-path config-path)
         {:nuzzle/publish-dir "/tmp/nuzzle-test-out",
          :nuzzle/base-url "https://foobar.com"
          :nuzzle/sitemap? true
          :nuzzle/build-drafts? true,
          :nuzzle/render-page 'nuzzle.config-test/render-page,
          :nuzzle/author-registry {:donna {:email "donnah@mail.com",
                                           :name "Donna Hayward",
                                           :url "https://donnahayward.com"},
                                   :josie {:name "Josie Packard"},
                                   :shelly {:email "shellyj@mail.com", :name "Shelly Johnson"}}
          :nuzzle/atom-feed {:author :donna,
                             :subtitle "Rants about foo and thoughts about bar",
                             :title "Foo's blog"}
          :nuzzle/overlay-dir "public",
          :meta {:twitter "https://twitter/foobar"},
          [] {:nuzzle/title "Home"},
          [:about] {:nuzzle/updated "2022-05-09T12:00Z",
                    :nuzzle/content "test-resources/markdown/about.md",
                    :nuzzle/title "About"},
          [:blog :favorite-color] {:nuzzle/content "test-resources/markdown/favorite-color.md",
                                   :nuzzle/updated "2022-05-09T12:00Z"
                                   :nuzzle/feed? true,
                                   :nuzzle/author :josie
                                   :nuzzle/tags #{:colors},
                                   :nuzzle/title "What's My Favorite Color? It May Suprise You."},
          [:blog :nuzzle-rocks] {:nuzzle/content "test-resources/markdown/nuzzle-rocks.md",
                                 :nuzzle/updated "2022-05-09T12:00Z",
                                 :nuzzle/author :shelly
                                 :nuzzle/feed? true,
                                 :nuzzle/tags #{:nuzzle},
                                 :nuzzle/title "10 Reasons Why Nuzzle Rocks"},
          [:blog :why-nuzzle] {:nuzzle/content "test-resources/markdown/why-nuzzle.md",
                               :nuzzle/updated "2022-05-09T12:00Z"
                               :nuzzle/feed? true,
                               :nuzzle/author :donna
                               :nuzzle/tags #{:nuzzle},
                               :nuzzle/title "Why I Made Nuzzle"}})))

(deftest validate-config
  (testing "bad config fails with expound output"
    (let [error-str (with-out-str (try (-> config-2-path
                                           conf/read-config-from-path
                                           conf/validate-config)
                                    (catch Throwable _ nil)))]
      (is (re-find #"Spec failed" error-str))
      (is (re-find #"should contain key:.{6}:nuzzle/base-url" error-str))))
  (testing "author registry author validation works"
    (let [config {:nuzzle/base-url "https://twin.peaks"
                  :nuzzle/render-page 'nuzzle.config-test/render-page
                  :nuzzle/author-registry {:laura {:name "Laura Palmer"}}
                  [:blog :douglas-firs] {:nuzzle/title "Douglas Firs Smell Really Freakin Good"
                                         :nuzzle/author :agent-cooper}}
          error-str (with-out-str (try (conf/validate-config config) (catch Throwable _ nil)))]
      (is (re-find #"Spec failed" error-str))
      (is (re-find #"->\W*config\W*:nuzzle\/author-registry\W*keys\W*set" error-str)))))

(deftest create-tag-index-page-entries
  (is (= {[:tags :bar]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/title "#bar"}
          [:tags :baz]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/title "#baz"}}
         (conf/create-tag-index-page-entries {[:blog :foo] {:nuzzle/tags #{:bar :baz}} [:about] {}})))
  (is (= {[:tags :nuzzle]
          {:nuzzle/index #{[:blog :nuzzle-rocks] [:blog :why-nuzzle]},
           :nuzzle/title "#nuzzle"}
          [:tags :colors]
          {:nuzzle/index #{[:blog :favorite-color]},
           :nuzzle/title "#colors"}}
         (conf/create-tag-index-page-entries (config)))))

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
         (conf/create-hierarchical-index-page-entries {[:blog-posts :foo] {:nuzzle/title "Foo"}
                                  [:blog-posts :archive :baz] {:nuzzle/title "Baz"}
                                  [:projects :bee] {:nuzzle/title "Bee"}})))
  (is (= {[:blog]
          {:nuzzle/index
           #{[:blog :why-nuzzle] [:blog :favorite-color] [:blog :nuzzle-rocks]},
           :nuzzle/title "Blog"}
          []
          {:nuzzle/index #{[:about] [:blog]}
           :nuzzle/title "Home"}}
         (conf/create-hierarchical-index-page-entries (config)))))
