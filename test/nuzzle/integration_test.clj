(ns nuzzle.integration-test
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.test :as t]
   [nuzzle.util :as util]))

(t/deftest example-sites
  (doseq [example-dir (-> "examples" io/file (.listFiles))]
    (let [prev-build-path (-> example-dir (fs/path "dist") fs/canonicalize str)
          prev-build-snapshot (util/create-dir-snapshot prev-build-path)
          new-build-path (str (fs/create-temp-dir))
          {:keys [exit out]} (sh/sh "bash" "-c"
                                    (str "cd " example-dir " && clj -T:site publish :publish-dir '\"" new-build-path "\"'"))
          new-build-snapshot (util/create-dir-snapshot new-build-path)
          diff (util/create-dir-diff prev-build-snapshot new-build-snapshot)]
      (t/is (zero? exit))
      (t/is (re-find #"Publishing successful" out))
      (t/is (every? #(-> % val empty?) diff))
      (println "diff -r" prev-build-path new-build-path))))
