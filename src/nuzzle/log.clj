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

(def info (log-gen (ansi/bold-green "INFO")))

(def warn (log-gen (ansi/bold-yellow "WARN")))

(def error (log-gen (ansi/bold-red "ERROR")))

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

(defn log-start-server [port]
  (info "Starting development server on port" port))

(defn log-rendering-page [url]
  (info "Rendering page:" (pr-str url)))
