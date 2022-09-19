(ns build
  (:require
   [babashka.process :as p]
   [babashka.process.pprint]
   [clojure.java.shell :as sh]
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

(defn update-example-deps []
  (->> (io/file "examples")
       file-seq
       (filter #(= "deps.edn" (.getName %)))
       (reduce
        (fn [_ file]
          (spit file
                (str/replace-first
                 (slurp file)
                 #"codes\.stel/nuzzle \{:mvn/version \"[0-9\.]+\"\}"
                 (str "codes.stel/nuzzle {:mvn/version \"" version "\"}"))))
        nil)))

(comment (update-example-deps))

(defn ensure-clean-tree [& _]
  (println "Checking if working tree is clean")
  (try
    (-> (p/process ["git" "diff-index" "--quiet" "HEAD" "--"]) p/check)
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
  (-> (p/process ["git" "tag" "-a" "-m" (str "Bump version to " git-tag) git-tag]
                 {:out :inherit :err :inherit})
      p/check)
  (println "Tagged latest commit with" git-tag))

(defn update-docs []
  (ensure-clean-tree)
  (update-example-deps)
  (render-templates)
  (-> (p/process ["git" "commit" "--all" "-m" (str "Update docs to version " version)]
                 {:out :inherit :err :inherit})
      p/check))

(defn ensure-tag [_]
  (when-not (re-find (re-pattern git-tag)
                     (b/git-process {:git-args "tag"}))
    (throw (ex-info (str "Tag " git-tag " not found in git tags") {}))))

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
  opts)
