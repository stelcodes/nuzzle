(ns nuzzle.generator-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [nuzzle.config :as conf]
            [nuzzle.generator :as gen]))

(def config-path "test-resources/edn/config-1.edn")

(deftest gen-get-config
  (let [config (conf/load-config-from-path config-path)
        get-config (gen/gen-get-config config)]
    (is (= "https://foobar.com" (get-config :nuzzle/base-url)))
    (is (= "https://twitter/foobar" (get-config :meta :twitter)))
    (is (= "/about/" (get-config [:about] :nuzzle/url)))
    (is (= #{[:blog :favorite-color] [:blog :nuzzle-rocks] [:blog :why-nuzzle]}
           (get-config [:blog] :nuzzle/index)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"get-config error: config key sequence \[:bad-key\] cannot be resolved"
                          (get-config :bad-key)))))

(comment (run-tests))
