(ns nuzzle.log
  (:require [clojure.string :as string]))

(defn log-time []
  (let [now (java.time.LocalDateTime/now)
        formatter (java.time.format.DateTimeFormatter/ofPattern "HH:mm:ss")]
    (.format now formatter)))

(defn info [& strs]
  (apply print (log-time) "INFO" strs))

(defn warn [& strs]
  (apply print (log-time) "WARN" strs))

(defn error [& strs]
  (apply print (log-time) "ERROR" strs))

(comment (info "test" "ok"))

