(ns build
  (:require
   [babashka.process :as p]
   [babashka.process.pprint]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.build.api :as b]))

(def lib 'codes.stel/nuzzle)
(def version (str (-> ".VERSION_PREFIX" slurp str/trim) "." (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(def git-tag (str "v" version))

(defn render-templates [& _]
  (println "Rendering templates")
  (letfn [(slurp-match [matches]
            (-> matches
                second
                slurp
                str/trim))
          (render-template [file]
            (spit (str/replace file ".template" "")
                  (-> file
                      slurp
                      (str/replace #"\{\{(\S+)\}\}" slurp-match))))]
    (render-template "README.template.md"))
  (println "Finished rendering templates"))

(comment (render-templates))

(defn get-latest-version []
  (->> @(p/process ["git" "describe" "--tags" "--abbrev=0"])
       :out
       slurp
       (re-find #"[0-9\.]+")))

(comment (get-latest-version))

(defn update-example-deps []
  (println "Updating example deps.edn files")
  (let [latest-version (get-latest-version)]
    (->> (io/file "examples")
         file-seq
         (filter #(= "deps.edn" (.getName %)))
         (reduce
          (fn [_ file]
            (spit file
                  (str/replace-first
                   (slurp file)
                   #"codes\.stel/nuzzle \{:mvn/version \"[0-9\.]+\"\}"
                   (str "codes.stel/nuzzle {:mvn/version \"" latest-version "\"}"))))
          nil)))
  (when (-> @(p/process ["git" "diff" "--quiet"]) :exit (= 1))
    (println "Commiting docs changes")
    (p/check (p/process ["git" "commit" "--all" "-m" "Update Nuzzle version in example deps.edn"]
                        {:out :inherit :err :inherit})))
  (println "Finished updating example deps.edn files"))

(comment (update-example-deps))

(defn ensure-clean-tree [& _]
  (println "Checking if working tree is clean")
  (try
    ;; Check if working tree has staged changes
    (p/check (p/process ["git" "diff-index" "--quiet" "--cached" "HEAD" "--"]))
    ;; Check if working tree has meaningful changes that could be staged
    ;; Not using git diff-files here bc it returns 0 when file metadata changed
    (p/check (p/process ["git" "diff" "--quiet"]))
    (catch Throwable e
      (println "Working tree is dirty")
      (throw e)))
  (println "Working tree is clean"))

(defn ensure-tests [& _]
  (println "Checking if tests pass")
  (p/check (p/process ["bb" "clojure" "-M:test"] {:out :inherit :err :inherit}))
  (println "Tests are passing"))

(defn ensure-lint [& _]
  (println "Checking for linter warnings")
  (p/check (p/process ["bb" "clojure" "-M:clj-kondo" "--lint" "src" "test"] {:out :inherit :err :inherit}))
  (println "No linter warnings"))

(defn tag-latest [& _]
  (println "Attempting to tag" git-tag)
  (if (= git-tag (-> @(p/process ["git" "describe" "--tags"]) :out slurp str/trim))
    (println "HEAD already has tag" git-tag)
    (do (println "Tagging latest commit")
      (p/check (p/process ["git" "tag" "-a" "-m" (str "Bump version to " version) git-tag]
                          {:out :inherit :err :inherit}))
      (println "Tagged latest commit with" git-tag))))

(defn update-templated-files [& _]
  (println "Updating templated files")
  (ensure-clean-tree)
  (render-templates)
  (when (-> @(p/process ["git" "diff" "--quiet"]) :exit (= 1))
    (println "Commiting templated file changes")
    (p/check (p/process ["git" "commit" "--all" "-m" "Update templated files"]
                        {:out :inherit :err :inherit})))
  (println "Finished updating templated files"))

(defn push-commits-and-tags [& _]
  (println "Pushing commits")
  (ensure-clean-tree)
  (p/check (p/process ["git" "push" "origin"] {:out :inherit :err :inherit}))
  (println "Pushing tags")
  (p/check (p/process ["git" "push" "--tags" "origin"] {:out :inherit :err :inherit}))
  (println "Finished pushing commits and tags"))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis}))

(defn deploy [opts]
  (ensure-clean-tree)
  (ensure-lint)
  (ensure-tests)
  (update-templated-files)
  (tag-latest)
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  (update-example-deps)
  (push-commits-and-tags)
  opts)
