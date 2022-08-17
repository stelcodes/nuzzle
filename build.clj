(ns build
  (:require
   [clojure.string :as str]
   [clojure.tools.build.api :as b]))

(def lib 'codes.stel/nuzzle)
(def version (str (-> ".VERSION_PREFIX" slurp str/trim) "." (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(def git-tag (str "v" version))

(defn tag [_]
  (when-not (re-find (re-pattern version) (slurp "README.md"))
    (throw (ex-info (str "Version " version " not found in README.md") {})))
  (b/git-process {:git-args (list "tag" "-a" "-m" (str "Bump version to " git-tag) git-tag)})
  (println "Tagged latest commit with" git-tag))

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
  (ensure-tag nil)
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
          opts))
  opts)
