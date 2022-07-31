(ns nuzzle.config-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]))

(def config-path "test-resources/edn/config-1.edn")

(defn render-page [_] (constantly [:h1 "test"]))

(deftest decode-config
  (is (= {:nuzzle/render-page render-page
          :nuzzle/base-url "http://foobar.com"
          :site-data #{{:id []
                        :modified (java.time.LocalDate/parse "2022-05-09")}}}
         (conf/decode-config
          {:nuzzle/render-page render-page
           :nuzzle/base-url "http://foobar.com"
           :site-data #{{:id []
                         :modified "2022-05-09"}}}))))

(deftest read-specified-config
  (is (= (conf/read-specified-config config-path {})
         {:nuzzle/publish-dir "/tmp/nuzzle-test-out",
          :server-port 6899,
          :nuzzle/base-url "https://foobar.com"
          :sitemap? true
          :nuzzle/build-drafts? true,
          :nuzzle/render-page render-page,
          :rss-channel
          {:title "Foo's blog",
           :description "Rants about foo and thoughts about bar",
           :link "https://foobar.com"}
          :overlay-dir "public",
          :site-data
          #{{:id []}
            {:id [:blog :nuzzle-rocks],
             :title "10 Reasons Why Nuzzle Rocks",
             :content "test-resources/markdown/nuzzle-rocks.md",
             :modified "2022-05-09"
             :rss? true,
             :tags #{:nuzzle}}
            {:id [:blog :why-nuzzle],
             :title "Why I Made Nuzzle",
             :content "test-resources/markdown/why-nuzzle.md",
             :rss? true,
             :tags #{:nuzzle}}
            {:id [:blog :favorite-color],
             :title "What's My Favorite Color? It May Suprise You.",
             :content "test-resources/markdown/favorite-color.md",
             :rss? true,
             :tags #{:colors}}
            {:id [:about],
             :title "About",
             :content "test-resources/markdown/about.md"}
            {:id :meta, :twitter "https://twitter/foobar"}}})))

