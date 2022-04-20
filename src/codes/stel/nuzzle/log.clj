(ns codes.stel.nuzzle.log
  (:require [clojure.string :as string]))

(defn log-time []
  (let [now (java.time.LocalDateTime/now)
        formatter (java.time.format.DateTimeFormatter/ofPattern "HH:mm:ss")]
    (.format now formatter)))

(defn info [& strs]
  (println (str (log-time) " INFO " (string/join \space strs))))

(defn warn [& strs]
  (println (str (log-time) " WARN " (string/join \space strs))))

(comment (info "test" "ok"))

