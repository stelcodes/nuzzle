(ns nuzzle.feed-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.feed :as feed]
   [nuzzle.generator :as gen]))

(defn config [] (conf/load-specified-config "test-resources/edn/config-1.edn"))

(deftest create-atom-feed
  (let [config (config)
        rendered-site-index (gen/generate-rendered-site-index config)]
    (is (= (-> "test-resources/sites/config-1-site/feed.xml" slurp str/trim)
           (feed/create-atom-feed config rendered-site-index {:deterministic? true})))))
