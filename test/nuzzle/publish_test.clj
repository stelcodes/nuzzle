(ns nuzzle.publish-test
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.log :as log]
   [nuzzle.publish :as publish]
   [nuzzle.util :as util]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/load-config-from-path config-path))

(deftest publish-feed
  (let [temp-dir (fs/create-temp-dir)
        feed-path (fs/path temp-dir "feed.xml")
        reference-feed-path (fs/path "test-resources/sites/config-1-site/feed.xml")
        config (assoc (config) :nuzzle/publish-dir (str temp-dir))
        rendered-site-index (conf/create-site-index config)]
    (publish/publish-atom-feed config rendered-site-index {:deterministic? true})
    (is (fs/exists? feed-path))
    (is (fs/exists? reference-feed-path))
    (is (= (-> feed-path str slurp str/trim)
           (-> reference-feed-path str slurp str/trim)))))

(deftest publish-sitemap
  (let [temp-dir (fs/create-temp-dir)
        sitemap-path (fs/path temp-dir "sitemap.xml")
        reference-sitemap-path (fs/path "test-resources/xml/config-1-sitemap.xml")
        config (assoc (config) :nuzzle/publish-dir (str temp-dir))
        rendered-site-index (conf/create-site-index config)]
    (publish/publish-sitemap config rendered-site-index)
    (is (fs/exists? sitemap-path))
    (is (fs/exists? reference-sitemap-path))
    (is (= (-> sitemap-path str slurp str/trim)
           (-> reference-sitemap-path str slurp str/trim)))))

(deftest publish-site
  (let [temp-site-dir (str (fs/create-temp-dir))
        reference-site-dir (str (fs/path "test-resources/sites/config-1-site"))
        config (-> (config)
                   (assoc :nuzzle/publish-dir (str temp-site-dir))
                   (update :nuzzle/overlay-dir #(str "test-resources/" %)))
        _ (publish/publish-site config {:deterministic? true})
        mismatches (util/diff-dirs temp-site-dir reference-site-dir)]
    (doseq [mismatch mismatches
            :let [rel->abs-path (fn [parent-dir path] (str parent-dir "/" path))
                  diff-cmd ["diff" (rel->abs-path temp-site-dir mismatch)
                            (rel->abs-path reference-site-dir mismatch)]
                  {:keys [out err]} (apply util/safe-sh diff-cmd)]]
      (log/warn "Found mismatch:" mismatch)
      (apply println "+" diff-cmd)
      (println out)
      (println err))
    (is (nil? mismatches))))
