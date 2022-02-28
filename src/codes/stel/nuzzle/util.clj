(ns codes.stel.nuzzle.util
  (:require [clojure.string :as string]))

;; Taken from https://clojuredocs.org/clojure.core/merge
(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn id->uri
  [id]
  {:pre [(vector? id)]}
  (str "/" (string/join "/" (map name id)) "/"))

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
