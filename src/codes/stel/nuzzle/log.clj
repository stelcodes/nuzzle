(ns codes.stel.nuzzle.log)

(defn log-time []
  (let [now (java.time.LocalDateTime/now)
        formatter (java.time.format.DateTimeFormatter/ofPattern "HH:mm:ss")]
    (.format now formatter)))

(defn info [& strs]
  (println (apply str (log-time) " INFO " strs)))

(comment (info "test"))

