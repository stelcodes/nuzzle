(ns nuzzle.util
  (:require
   [babashka.fs :as fs]
   [clj-commons.digest :as digest]
   [clojure.java.shell :as sh]
   [clojure.pprint :as pprint]
   [clojure.set :as set]
   [clojure.string :as str]
   [nuzzle.schemas :as schemas]))

(defn spy [x] (pprint/pprint x) x)

;; Taken from https://clojuredocs.org/clojure.core/merge
(defn deep-merge
  {:malli/schema [:=> [:cat [:* map?]] map?]}
  [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn stringify-url
  {:malli/schema [:=> [:cat schemas/vec-url] string?]}
  [url]
  {:pre [(vector? url)]}
  (if (= [] url) "/"
    (str "/" (str/join "/" (map name url)) "/")))

(defn vectorize-url
  {:malli/schema [:=> [:cat string?] schemas/vec-url]}
  [url]
  {:pre [(string? url)]}
  (->> (str/split url #"/")
       (remove str/blank?)
       (map keyword)
       vec))

(defn kebab-case->title-case
  [s]
  (->> (str/split (name s) #"-")
       (map str/capitalize)
       (str/join " ")))

(comment
 (str/split (name :educational-media) #"-")
 (kebab-case->title-case :educational-media))

(defn kebab-case->lower-case
  [s]
  (->> (str/split (name s) #"-")
       (map str/lower-case)
       (str/join " ")))

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
  {:malli/schema [:=> [:cat vector? schemas/regex?] string?]}
  [hiccup regex]
  (reduce
   (fn [_ item]
     (let [desc-result (and (vector? item) (find-hiccup-str item regex))]
       (or (and (string? item) (re-find regex item) (reduced item))
           (and desc-result (reduced desc-result)))))
   hiccup))

(defn path->last-mod-inst
  "Returns Instant of last modified time of a file specified by path"
  {:malli/schema [:=> [:cat string?] inst?]}
  [path]
  (-> path
      fs/last-modified-time
      fs/file-time->instant
      (.truncatedTo java.time.temporal.ChronoUnit/SECONDS)))

(comment (path->last-mod-inst "deps.edn"))

(defn generate-chroma-command
  [file-path language & {:keys [style line-numbers?]}]
  (remove nil?
          ["chroma" (str "--lexer=" language) "--formatter=html" "--html-only"
           "--html-prevent-surrounding-pre" (when style "--html-inline-styles")
           (when style (str "--style=" (name style))) (when line-numbers? "--html-lines") file-path]))

(defn generate-pygments-command
  [file-path language & {:keys [style line-numbers?]}]
  (let [;; TODO: turn nowrap on for everything if they release my PR
        ;; https://github.com/pygments/pygments/issues/2127
        options (remove nil?
                        [(when-not line-numbers? "nowrap") (when style "noclasses")
                         (when style (str "style=" (name style)))
                         (when line-numbers? "linenos=inline")])]
    ["pygmentize" "-f" "html" "-l" language "-O" (str/join "," options) file-path]))

(defn child-url?
  {:malli/schema [:=> [:cat schemas/vec-url schemas/vec-url] [:or boolean? nil?]]}
  [parent-url child-url]
  (let [parent-count (count parent-url)
        child-count (count child-url)]
    (when (= 1 (- child-count parent-count))
      (every? #(= (nth child-url %) (nth parent-url %))
              (range parent-count)))))

(defn create-dir-diff
  {:malli/schema [:=> [:cat schemas/dir-snapshot schemas/dir-snapshot] schemas/dir-diff]}
  [old-snapshot new-snapshot]
  (let [added (set/difference (set (keys new-snapshot)) (set (keys old-snapshot)))
        removed (set/difference (set (keys old-snapshot)) (set (keys new-snapshot)))
        remaining (set/difference (set (keys old-snapshot)) added removed)
        changed (->> remaining
                     (filter #(not= (get old-snapshot %) (get new-snapshot %)))
                     set)]
      {:added added
       :removed removed
       :changed changed}))

(defn create-dir-snapshot
  {:malli/schema [:=> [:cat string?] schemas/dir-snapshot]}
  [dir]
  (let [dir (fs/file dir)
        path-len (count (-> dir fs/path str))
        path-from-dir #(subs (-> % fs/path str) path-len)]
    (->> (file-seq dir)
         (filter #(re-find #"\.[^.]+$" (path-from-dir %)))
         (map (juxt path-from-dir digest/md5))
         (into {}))))

(defn replace-in-file!
  {:malli/schema [:=> [:cat [:alt string? schemas/regex?] [:alt string? fn?]] nil?]}
  [file replace-arg replace-arg-2]
  (spit file
        (-> file
            slurp
            (str/replace replace-arg replace-arg-2))))
