(ns nuzzle.log
  (:require
   [babashka.fs :as fs]
   [io.aviso.ansi :as ansi]
   [nuzzle.util :as util]))

(defn log-gen [level]
  (fn [& items]
    (->> items
         (map str)
         (apply println (str (util/now-trunc-sec)) level))))

#_{:clj-kondo/ignore [:unresolved-var]}
(defn green [& args] (->> args (apply str) ansi/green))
#_{:clj-kondo/ignore [:unresolved-var]}
(defn yellow [& args] (->> args (apply str) ansi/yellow))
#_{:clj-kondo/ignore [:unresolved-var]}
(defn blue [& args] (->> args (apply str) ansi/blue))
#_{:clj-kondo/ignore [:unresolved-var]}
(defn red [& args] (->> args (apply str) ansi/red))
#_{:clj-kondo/ignore [:unresolved-var]}
(defn magenta [& args] (->> args (apply str) ansi/magenta))
#_{:clj-kondo/ignore [:unresolved-var]}
(defn cyan [& args] (->> args (apply str) ansi/cyan))

(def info (log-gen (green "INFO")))

(def warn (log-gen (yellow "WARN")))

(def error (log-gen (red "ERROR")))


(comment (info "test" "ok"))

(defn log-overlay-dir [overlay-dir]
  (info "Using overlay directory:" (fs/canonicalize overlay-dir)))

(defn log-feed [feed-file]
  (info "Creating Atom feed file:" (fs/canonicalize feed-file)))

(defn log-sitemap [sitemap-file]
  (info "Creating sitemap file:" (fs/canonicalize sitemap-file)))

(defn log-publish-start [publish-dir]
  (info "Publishing static site to:" (fs/canonicalize publish-dir)))

(defn log-publish-end []
  (info "Publishing successful"))

(defn log-site-server [port]
  (info "Starting static site server on port" port))

(defn log-nrepl-server [port]
  (info "Starting nREPL server on port" port))

(defn log-rendering-page [url]
  (info "Rendering page:" (pr-str url)))
