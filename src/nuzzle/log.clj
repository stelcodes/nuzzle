(ns nuzzle.log
  (:require
   [babashka.fs :as fs]
   [clojure.pprint :as pp]))

(defn log-time []
  (let [now (java.time.LocalDateTime/now)
        formatter (java.time.format.DateTimeFormatter/ofPattern "HH:mm:ss")]
    (.format now formatter)))

(defn log-gen [level]
  (fn [& items]
    (->> items
         (map str)
         (apply println (log-time) level))))

(def info (log-gen "INFO"))

(def warn (log-gen "WARN"))

(def error (log-gen "ERROR"))

(comment (info "test" "ok"))

(defn log-overlay-dir [overlay-dir]
  (info "💎🐈 Using overlay directory:" (fs/canonicalize overlay-dir)))

(defn log-remove-drafts []
  (info "❌🐈 Removing drafts"))

(defn log-build-drafts []
  (info "🔨🐈 Building drafts"))

(defn log-feed [feed-file]
  (info "📰🐈 Creating Atom feed file:" (fs/canonicalize feed-file)))

(defn log-sitemap [sitemap-file]
  (info "📖🐈 Creating sitemap file:" (fs/canonicalize sitemap-file)))

(defn log-publish-start [publish-dir]
  (info "💫🐈 Publishing static site to:" (fs/canonicalize publish-dir)))

(defn log-publish-end []
  (info "✅🐈 Publishing successful"))

(defn log-start-server [port]
  (info "✨🐈 Starting development server on port" port))

(defn log-rendering-page [page]
  (info "⚡🐈 Rendering page:")
  (->> page (into (sorted-map)) pp/pprint))
