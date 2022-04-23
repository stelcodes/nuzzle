(ns nuzzle.rss-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.rss :as rss]
   [nuzzle.generator :as gen]))

(def config-path "test-resources/edn/config-1.edn")

(def config (gen/load-specified-config config-path {}))

(deftest create-rss-feed
  (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"><channel><atom:link href=\"https://foobar.com\" rel=\"self\" type=\"application/rss+xml\"/><title>Foo's blog</title><description>Rants about foo and thoughts about bar</description><link>https://foobar.com</link><generator>clj-rss</generator><item><title>Why I Made Nuzzle</title><guid isPermaLink=\"false\">https://foobar.com/blog/why-nuzzle/</guid></item><item><title>What's My Favorite Color? It May Suprise You.</title><guid isPermaLink=\"false\">https://foobar.com/blog/favorite-color/</guid></item><item><title>10 Reasons Why Nuzzle Rocks</title><guid isPermaLink=\"false\">https://foobar.com/blog/nuzzle-rocks/</guid></item></channel></rss>"
         (rss/create-rss-feed
          (gen/realize-site-data config) (:rss-opts config)))))
