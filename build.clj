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
    (render-template "README.template.md")))

(comment (render-templates))

(defn get-latest-version []
  (->> (p/sh ["git" "describe" "--tags" "--abbrev=0"])
       :out
       (re-find #"[0-9\.]+")))

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
          nil))))

(comment (update-example-deps))

(defn ensure-clean-tree [& _]
  (println "Checking if working tree is clean")
  (try
    ;; Check if working tree has staged changes
    (-> (p/process ["git" "diff-index" "--quiet" "--cached" "HEAD" "--"]) p/check)
    ;; Check if working tree has meaningful changes that could be staged
    ;; Not using git diff-files here bc it returns 0 when file metadata changed
    (-> (p/process ["git" "diff" "--quiet"]) p/check)
    (catch Throwable e
      (println "Working tree is dirty")
      (throw e)))
  (println "Working tree is clean"))

(defn ensure-tests [& _]
  (println "Checking if tests pass")
  (-> (p/process ["clj" "-M:test"] {:out :inherit :err :inherit})
      p/check)
  (println "Tests are passing"))

(defn tag-head [& _]
  ;; Make sure README is up to date before tagging
  (render-templates)
  (ensure-clean-tree)
  ;; Tag HEAD commit
  (-> (p/process ["git" "tag" "-a" "-m" (str "Bump version to " version) git-tag]
                 {:out :inherit :err :inherit})
      p/check)
  (println "Tagged latest commit with" git-tag))

(defn update-docs [& _]
  (println "Updating docs")
  (ensure-clean-tree)
  (update-example-deps)
  (render-templates)
  (when (-> (p/process ["git" "diff-files" "--quiet"]) :exit (= 1))
    (println "Commiting docs changes")
    (-> (p/process ["git" "commit" "--all" "-m" (str "Update docs to version " (get-latest-version))]
                   {:out :inherit :err :inherit})
        p/check)))

(defn push-commits-and-tags [& _]
  (println "Pushing commits")
  (ensure-clean-tree)
  (-> (p/process ["git" "push" "origin"] {:out :inherit :err :inherit})
      p/check)
  (println "Pushing tags")
  (-> (p/process ["git" "push" "--tags" "origin"] {:out :inherit :err :inherit})
      p/check))

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
  (ensure-tests)
  (tag-head)
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  (update-docs)
  (push-commits-and-tags)
  opts)
