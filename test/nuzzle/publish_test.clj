(ns nuzzle.publish-test
  (:require
   [babashka.fs :as fs]
   [clj-commons.digest :as digest]
   [clojure.data :as data]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [nuzzle.pages :as pages]
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
         (publish/create-sitemap {} {}))))

(deftest publish-site
  (let [temp-site-dir (str (fs/create-temp-dir))
        reference-site-dir (str (fs/path "test-resources/sites/twin-peaks"))
        pages (-> test-util/twin-peaks-pages pages/load-pages)
        atom-feed {:title "Foo's blog"
                   :author (test-util/authors :donna)
                   :subtitle "Rants about foo and thoughts about bar"}
        _ (publish/publish-site pages :overlay-dir "test-resources/public" :deterministic? true
                                :base-url "https://foobar.com" :publish-dir (str temp-site-dir)
                                :atom-feed atom-feed)
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
