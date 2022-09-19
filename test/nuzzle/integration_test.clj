(ns nuzzle.integration-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t]
   [nuzzle.util :as util]))

(t/deftest example-sites
  (doseq [example-dir-file (-> "examples" io/file (.listFiles))]
    (t/testing (str "example site " example-dir-file)
      (let [cwd (-> "." fs/canonicalize str)
            example-path (-> example-dir-file fs/canonicalize str)
            example-dist-path (str example-path "/dist")
            dist-snapshot (util/create-dir-snapshot example-dist-path)
            new-example-path (str (fs/create-temp-dir))
            new-example-dist-path (str new-example-path "/dist")
            _ (fs/copy-tree example-path new-example-path)
            new-deps-path (str new-example-path "/deps.edn")
            _ (spit new-deps-path
                    (-> new-deps-path
                        slurp
                        (str/replace #"codes\.stel/nuzzle \{:mvn/version \"[0-9\.]+\"\}"
                                     (str "codes.stel/nuzzle {:local/root " (pr-str cwd) "}"))))
            ;; Print both out and err to *out* so kaocha will print it upon test failure
            {:keys [exit]} @(p/process ["bb" "clojure" "-T:site" "publish"] {:dir new-example-path
                                                                             :out *out*
                                                                             :err *out*})
            new-dist-snapshot (util/create-dir-snapshot new-example-dist-path)
            dist-diff (-> (util/create-dir-diff dist-snapshot new-dist-snapshot)
                          ;; The atom feed will always have different creation times
                          ;; TODO: use str/replace to replace creation time
                          (update :changed #(disj % "/feed.xml")))]
        (t/is (zero? exit))
        (t/is (every? #(-> % val empty?) dist-diff))
        (if (and (zero? exit)
                 (every? #(-> % val empty?) dist-diff))
          (fs/delete-tree new-example-path)
          (println "diff -r" example-dist-path new-example-dist-path "\n"
                   "caddy file-server --listen :3030 --root" new-example-dist-path "\n"
                   "trash" example-dist-path "; cp -R" new-example-dist-path example-dist-path))))))
