(ns nuzzle.log)

(defn log-time []
  (let [now (java.time.LocalDateTime/now)
        formatter (java.time.format.DateTimeFormatter/ofPattern "HH:mm:ss")]
    (.format now formatter)))

(defn info [& strs]
  (apply println (log-time) "INFO" strs))

(defn warn [& strs]
  (apply println (log-time) "WARN" strs))

(defn error [& strs]
  (apply println (log-time) "ERROR" strs))

(comment (info "test" "ok"))

