(ns nuzzle.util-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.util :as util]))

(deftest uri->id
  (is (= (list [:about] [] [:blog-posts :foo])
         (map util/uri->id ["/about/" "/" "/blog-posts/foo/"]))))

(deftest format-simple-date
  (is (= "2016-04-03"
         (util/format-simple-date (java.time.LocalDateTime/parse "2016-04-03T10:15:30"))))
  (is (= "2016-04-03"
         (util/format-simple-date (java.time.LocalDate/parse "2016-04-03")))))
