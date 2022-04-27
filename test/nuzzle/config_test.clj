(ns nuzzle.config-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]))

(def config-path "test-resources/edn/config-1.edn")

(defn render-webpage [_] (constantly [:h1 "test"]))

(deftest read-specified-config
  (is (= (conf/read-specified-config config-path {})
         {:export-dir "/tmp/nuzzle-test-out",
          :dev-port 6899,
          :remove-drafts? false,
          :render-webpage render-webpage,
          :rss-channel
          {:title "Foo's blog",
           :description "Rants about foo and thoughts about bar",
           :link "https://foobar.com"}
          :overlay-dir "public",
          :site-data
          [{:id []}
           {:id [:blog :nuzzle-rocks],
            :title "10 Reasons Why Nuzzle Rocks",
            :markdown "test-resources/markdown/nuzzle-rocks.md",
            :rss? true,
            :tags [:nuzzle]}
           {:id [:blog :why-nuzzle],
            :title "Why I Made Nuzzle",
            :markdown "test-resources/markdown/why-nuzzle.md",
            :rss? true,
            :tags [:nuzzle]}
           {:id [:blog :favorite-color],
            :title "What's My Favorite Color? It May Suprise You.",
            :markdown "test-resources/markdown/favorite-color.md",
            :rss? true,
            :tags [:colors]}
           {:id [:about],
            :title "About",
            :markdown "test-resources/markdown/about.md"}
           {:id :meta, :twitter "https://twitter/foobar"}]})))
