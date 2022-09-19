(ns nuzzle.integration-test
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.test :as t]
   [nuzzle.util :as util]))

(t/deftest example-sites
  (doseq [example (-> "examples" io/file (.listFiles))]
    (let [cwd (-> "." fs/canonicalize str)
          example-path (-> example fs/canonicalize str)
          dist-snapshot (util/create-dir-snapshot (str example-path "/dist"))
          new-example-path (str (fs/create-temp-dir))
          _ (fs/copy-tree example-path new-example-path)
          new-deps-path (str new-example-path "/deps.edn")
          _ (spit new-deps-path
                  (-> new-deps-path
                      slurp
                      (str/replace #"codes\.stel/nuzzle \{:mvn/version \"[0-9\.]+\"\}"
                                   (str "codes.stel/nuzzle {:local/root " (pr-str cwd) "}"))))
          {:keys [exit]} (sh/sh "bash" "-c"
                                (str "cd " new-example-path " && clj -T:site publish"))
          new-dist-snapshot (util/create-dir-snapshot (str new-example-path "/dist"))
          dist-diff (util/create-dir-diff dist-snapshot new-dist-snapshot)]
      (t/is (zero? exit))
      (t/is (every? #(-> % val empty?) dist-diff))
      (if (and (zero? exit)
               (every? #(-> % val empty?) dist-diff))
        (fs/delete-tree new-example-path)
        (println "diff -r" example-path new-example-path)))))
