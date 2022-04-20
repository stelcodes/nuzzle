(ns nuzzle.util
  (:require
   [babashka.fs :as fs]
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

(defn remove-nil-values
  "Removes kv-pairs of a map where the value is nil."
  [x]
  {:pre [(map? x)]}
  (reduce-kv
   (fn [m k v] (if (nil? v) m (assoc m k v)))
   {}
   x))

(defn ensure-static-dir
  [static-dir-path]
  fs/canonicalize
  (when (not (fs/directory? static-dir-path))
    (throw (ex-info (str "Static directory " (fs/canonicalize static-dir-path) " does not exist")
                    {:static-dir static-dir-path}))))
