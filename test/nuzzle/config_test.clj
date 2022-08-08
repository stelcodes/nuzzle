(ns nuzzle.config-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]))

(def config-path "test-resources/edn/config-1.edn")

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
          [] {:title "Home"},
          [:about] {:content "test-resources/markdown/about.md", :title "About"},
          [:blog :favorite-color] {:content "test-resources/markdown/favorite-color.md",
                                   :rss? true,
                                   :tags #{:colors},
                                   :title "What's My Favorite Color? It May Suprise You."},
          [:blog :nuzzle-rocks] {:content "test-resources/markdown/nuzzle-rocks.md",
                                 :modified "2022-05-09",
                                 :rss? true,
                                 :tags #{:nuzzle},
                                 :title "10 Reasons Why Nuzzle Rocks"},
          [:blog :why-nuzzle] {:content "test-resources/markdown/why-nuzzle.md",
                               :rss? true,
                               :tags #{:nuzzle},
                               :title "Why I Made Nuzzle"}})))

