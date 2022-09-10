(ns nuzzle.util
  (:require
   [babashka.fs :as fs]
   [clj-commons.digest :as digest]
   [clojure.data :as data]
   [clojure.java.shell :as sh]
   [clojure.pprint :as pprint]
   [clojure.string :as string]))

(defn spy [x] (pprint/pprint x) x)

;; Taken from https://clojuredocs.org/clojure.core/merge
(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn page-key->url
  [page-key]
  {:pre [(vector? page-key)]}
  (if (= [] page-key) "/"
    (str "/" (string/join "/" (map name page-key)) "/")))

(defn url->page-key
  [url]
  {:pre [(string? url)]}
  (->> (string/split url #"/")
       (remove string/blank?)
       (map keyword)
       vec))

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

(defn ensure-overlay-dir
  [overlay-dir-path]
  (when (not (fs/directory? overlay-dir-path))
    (throw (ex-info (str "Overlay directory " (fs/canonicalize overlay-dir-path) " does not exist")
                    {:nuzzle/overlay-dir overlay-dir-path}))))

(defn safe-sh [command & args]
  (try (apply sh/sh command (remove nil? args))
    (catch Exception _
      {:exit 1 :err (str "Command failed. Please ensure " command " is installed.")})))

(defn time-str->?inst
  "Converts date, datetime, or zoned datetime string into Instant. Returns nil
  if conversion cannot be done"
  [time-str]
  (letfn [(parse-date [ts] (try (-> (java.time.LocalDate/parse ts)
                                    (.atStartOfDay (java.time.ZoneId/systemDefault))
                                    (.toInstant))
                             (catch Throwable _ nil)))
          (parse-datetime [ts] (try (-> (java.time.LocalDateTime/parse ts)
                                        (.atZone (java.time.ZoneId/systemDefault))
                                        (.toInstant))
                                 (catch Throwable _ nil)))
          (parse-zoned-datetime [ts] (try (-> (java.time.ZonedDateTime/parse ts)
                                              (.toInstant))
                                       (catch Throwable _ nil)))]
    (or (parse-date time-str) (parse-datetime time-str) (parse-zoned-datetime time-str))))

(defn now-trunc-sec []
  (.truncatedTo (java.time.Instant/now) java.time.temporal.ChronoUnit/SECONDS))

(defn find-hiccup-str
  "Find first string matching regular expression in deeply nested Hiccup tree"
  [regex hiccup]
  (reduce
   (fn [_ item]
     (let [desc-result (and (vector? item) (find-hiccup-str regex item))]
       (or (and (string? item) (re-find regex item) (reduced item))
           (and desc-result (reduced desc-result)))))
   hiccup))

(defn dir-contents-map
  "Create a datastructure representing a directory's structure and contents in
  order to compare it with another directory. Creates a map of: relative paths
  (string) -> md5 checksums (string) of all the files inside the directory.
  Nested directory paths are not checksummed and have a value of :dir"
  [dir]
  {:pre [(-> dir (java.io.File.) (.isDirectory))]}
  (let [files (-> dir (java.io.File.) file-seq)
        parent-path-name-count (-> files first (.toPath) (.getNameCount))]
    (reduce (fn [contents-map file]
              (let [abs-path (.toPath file)
                    path-name-count (.getNameCount abs-path)
                    rel-path (str (.subpath abs-path parent-path-name-count path-name-count))
                    md5-checksum (if (.isDirectory (.toFile abs-path))
                                   :dir
                                   (digest/md5 file))]
                (assoc contents-map rel-path md5-checksum)))
            (sorted-map) (rest files))))

(defn diff-dirs
  "Determine if two directories have the same file structure and content. If
  differences, return list of relative filenames that are different and log
  diffs. If identical, return nil"
  [dir1 dir2]
  (let [cm1 (dir-contents-map dir1)
        cm2 (dir-contents-map dir2)
        [d1 d2 _] (data/diff cm1 cm2)
        mismatches (-> (merge d1 d2) keys)]
    mismatches))

(defn path->last-mod-inst
  "Returns Instant of last modified time of a file specified by path"
  [path]
  (-> path
      fs/last-modified-time
      fs/file-time->instant
      (.truncatedTo java.time.temporal.ChronoUnit/SECONDS)))

(comment (path->last-mod-inst "deps.edn"))
