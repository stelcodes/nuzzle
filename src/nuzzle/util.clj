(ns nuzzle.util
  (:require
   [babashka.fs :as fs]
   [clojure.java.shell :as sh]
   [clojure.string :as string]))

;; Taken from https://clojuredocs.org/clojure.core/merge
(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn id->uri
  [id]
  {:pre [(vector? id)]}
  (if (= [] id) "/"
    (str "/" (string/join "/" (map name id)) "/")))

(defn kebab-case->title-case
  [s]
  (->> (string/split (name s) #"-")
       (map string/capitalize)
       (string/join " ")))

(comment
 (string/split (name :educational-media) #"-")
 (kebab-case->title-case :educational-media))

(defn kebab-case->lower-case
  [s]
  (->> (string/split (name s) #"-")
       (map string/lower-case)
       (string/join " ")))

(defn prune-map
  "Removes kv-pairs of a map where the value is nil."
  [x]
  {:pre [(map? x)] :post [#(map? %)]}
  (into {} (remove (comp nil? val) x)))

(defn ensure-static-dir
  [static-dir-path]
  fs/canonicalize
  (when (not (fs/directory? static-dir-path))
    (throw (ex-info (str "Static directory " (fs/canonicalize static-dir-path) " does not exist")
                    {:static-dir static-dir-path}))))

(defn safe-sh [[command & _ :as args]]
  (try (apply sh/sh args)
    (catch Exception _
      {:exit 1 :err (str "Command failed. Please ensure " command " is installed.")})))
