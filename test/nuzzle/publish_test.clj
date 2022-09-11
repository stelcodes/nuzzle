(ns nuzzle.publish-test
  (:require
   [babashka.fs :as fs]
   [clj-commons.digest :as digest]
   [clojure.data :as data]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.log :as log]
   [nuzzle.publish :as publish]
   [nuzzle.test-util :as test-util]
   [nuzzle.util :as util]))

(defn dir-contents-map
  "Create a datastructure representing a directory's structure and contents in
  order to compare it with another directory. Creates a map of: relative paths
  (string) -> md5 checksums (string) of all the files inside the directory.
  Nested directory paths are not checksummed and have a value of :dir"
  [dir]
  {:pre [(-> dir (java.io.File.) (.isDirectory))]}
  (let [files (-> dir (java.io.File.) file-seq)
        parent-path-name-count (-> files first (.toPath) (.getNameCount))]
    (reduce (fn [contents-map file]
              (let [abs-path (.toPath file)
                    path-name-count (.getNameCount abs-path)
                    rel-path (str (.subpath abs-path parent-path-name-count path-name-count))
                    md5-checksum (if (.isDirectory (.toFile abs-path))
                                   :dir
                                   (digest/md5 file))]
                (assoc contents-map rel-path md5-checksum)))
            (sorted-map) (rest files))))

(defn diff-dirs
  "Determine if two directories have the same file structure and content. If
  differences, return list of relative filenames that are different and log
  diffs. If identical, return nil"
  [dir1 dir2]
  (let [cm1 (dir-contents-map dir1)
        cm2 (dir-contents-map dir2)
        [d1 d2 _] (data/diff cm1 cm2)
        mismatches (-> (merge d1 d2) keys)]
    mismatches))

(deftest create-sitemap
  (is (= (-> "test-resources/xml/empty-sitemap.xml" slurp str/trim)
         (publish/create-sitemap {} {})))
  (let [loaded-config-1 (conf/load-config test-util/config-1)]
    (is (= (-> "test-resources/xml/config-1-sitemap.xml" slurp str/trim)
         (publish/create-sitemap loaded-config-1 (conf/create-site-index loaded-config-1) :base-url "https://foobar.com"))))
  (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a:urlset xmlns:a=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><a:url><a:loc>https://foo.com/</a:loc></a:url><a:url><a:loc>https://foo.com/about/</a:loc><a:lastmod>2022-05-09T12:00:00Z</a:lastmod></a:url><a:url><a:loc>https://foo.com/blog-posts/foobar/</a:loc></a:url></a:urlset>"
         (publish/create-sitemap {[:about] {:nuzzle/updated (util/time-str->?inst "2022-05-09T12:00Z")}}
                                 {"/" []
                                  "/about/" []
                                  "/blog-posts/foobar/" []}
                                 :base-url "https://foo.com"))))

(comment (spit "test-resources/xml/empty-sitemap.xml" (publish/create-sitemap {} {})))
(comment (spit "test-resources/xml/config-1-sitemap.xml" (publish/create-sitemap test-util/config-1 (conf/create-site-index test-util/config-1))))

(deftest create-atom-feed
  (let [config (conf/load-config test-util/config-1)
        rendered-site-index (conf/create-site-index config)]
    (is (= (-> "test-resources/sites/config-1-site/feed.xml" slurp str/trim)
           (publish/create-atom-feed config rendered-site-index :base-url "https://foobar.com" :deterministic? true)))))

(deftest publish-feed
  (let [temp-dir (fs/create-temp-dir)
        feed-path (fs/path temp-dir "feed.xml")
        reference-feed-path (fs/path "test-resources/sites/config-1-site/feed.xml")
        config (-> test-util/config-1 (assoc :nuzzle/publish-dir (str temp-dir)) conf/load-config)
        rendered-site-index (conf/create-site-index config)]
    (publish/publish-atom-feed config rendered-site-index :deterministic? true :base-url "https://foobar.com")
    (is (fs/exists? feed-path))
    (is (fs/exists? reference-feed-path))
    (is (= (-> feed-path str slurp str/trim)
           (-> reference-feed-path str slurp str/trim)))))

(deftest publish-sitemap
  (let [temp-dir (fs/create-temp-dir)
        sitemap-path (fs/path temp-dir "sitemap.xml")
        reference-sitemap-path (fs/path "test-resources/xml/config-1-sitemap.xml")
        config (-> test-util/config-1 (assoc :nuzzle/publish-dir (str temp-dir)) conf/load-config)
        rendered-site-index (conf/create-site-index config)]
    (publish/publish-sitemap config rendered-site-index :base-url "https://foobar.com")
    (is (fs/exists? sitemap-path))
    (is (fs/exists? reference-sitemap-path))
    (is (= (-> sitemap-path str slurp str/trim)
           (-> reference-sitemap-path str slurp str/trim)))))

(deftest publish-site
  (let [temp-site-dir (str (fs/create-temp-dir))
        reference-site-dir (str (fs/path "test-resources/sites/config-1-site"))
        config (-> test-util/config-1
                   (assoc :nuzzle/publish-dir (str temp-site-dir))
                   conf/load-config)
        _ (publish/publish-site config :overlay-dir "test-resources/public" :deterministic? true :base-url "https://foobar.com")
        mismatches (diff-dirs temp-site-dir reference-site-dir)]
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
