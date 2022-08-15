(ns nuzzle.config-test
  (:require
   [clojure.test :refer [deftest is]]
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
          :nuzzle/rss-channel {:title "Foo's blog",
                               :description "Rants about foo and thoughts about bar",
                               :link "https://foobar.com"}
          :nuzzle/overlay-dir "public",
          :meta {:twitter "https://twitter/foobar"},
          [] {:nuzzle/title "Home"},
          [:about] {:nuzzle/content "test-resources/markdown/about.md", :nuzzle/title "About"},
          [:blog :favorite-color] {:nuzzle/content "test-resources/markdown/favorite-color.md",
                                   :nuzzle/rss? true,
                                   :nuzzle/tags #{:colors},
                                   :nuzzle/title "What's My Favorite Color? It May Suprise You."},
          [:blog :nuzzle-rocks] {:nuzzle/content "test-resources/markdown/nuzzle-rocks.md",
                                 :nuzzle/updated "2022-05-09",
                                 :nuzzle/rss? true,
                                 :nuzzle/tags #{:nuzzle},
                                 :nuzzle/title "10 Reasons Why Nuzzle Rocks"},
          [:blog :why-nuzzle] {:nuzzle/content "test-resources/markdown/why-nuzzle.md",
                               :nuzzle/rss? true,
                               :nuzzle/tags #{:nuzzle},
                               :nuzzle/title "Why I Made Nuzzle"}})))

(deftest validate-config
  (let [config-2 (conf/read-config-path config-2-path)
        error-str (with-out-str (try (conf/validate-config config-2) (catch Throwable _ nil)))]
    (spit "/tmp/poop.txt" (pr-str error-str))
    (is (re-find #"Spec failed" error-str))
    (is (re-find #"should contain key:.{6}:nuzzle/base-url" error-str))))
