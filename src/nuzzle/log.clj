(ns nuzzle.log
  (:require
   [babashka.fs :as fs]))

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

(defn log-rss [rss-file]
  (info "📰🐈 Creating RSS file:" (fs/canonicalize rss-file)))

(defn log-export-start [export-dir]
  (info "🔨🐈 Exporting static site to:" (fs/canonicalize export-dir)))

(defn log-export-end []
  (info "✅🐈 Export successful"))

(defn log-start-server [dev-port]
  (info "✨🐈 Starting development server on port " dev-port))
