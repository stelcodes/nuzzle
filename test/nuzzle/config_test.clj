(ns nuzzle.config-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [nuzzle.config :as conf]))

(def config-path "test-resources/edn/config-1.edn")

(def config-2-path "test-resources/edn/config-2-bad.edn")

(def render-page (constantly [:h1 "test"]))

(deftest read-config-path
  (is (= (conf/read-config-path config-path)
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
    (let [config-2 (conf/read-config-path config-2-path)
          error-str (with-out-str (try (conf/validate-config config-2) (catch Throwable _ nil)))]
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
