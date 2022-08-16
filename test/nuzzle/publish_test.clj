(ns nuzzle.publish-test
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.generator :as gen]
   [nuzzle.log :as log]
   [nuzzle.publish :as publish]
   [nuzzle.util :as util]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/load-specified-config config-path {}))

(deftest publish-rss
  (let [temp-dir (fs/create-temp-dir)
        rss-path (fs/path temp-dir "feed.xml")
        reference-rss-path (fs/path "test-resources/sites/config-1-site/feed.xml")
        config (assoc (config) :nuzzle/publish-dir (str temp-dir))]
    (publish/publish-rss config)
    (is (fs/exists? rss-path))
    (is (fs/exists? reference-rss-path))
    (is (= (-> rss-path str slurp str/trim)
           (-> reference-rss-path str slurp str/trim)))))

(deftest publish-sitemap
  (let [temp-dir (fs/create-temp-dir)
        sitemap-path (fs/path temp-dir "sitemap.xml")
        reference-sitemap-path (fs/path "test-resources/xml/config-1-sitemap.xml")
        config (assoc (config) :nuzzle/publish-dir (str temp-dir))
        rendered-site-index (gen/generate-rendered-site-index config)]
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
        _ (publish/publish-site config)
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
