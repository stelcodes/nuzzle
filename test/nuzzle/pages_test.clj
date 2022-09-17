(ns nuzzle.pages-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.pages :as pages]
   [nuzzle.test-util :as test-util]))

;; (deftest validate-config
;;   (testing "bad config fails with expound output"
;;     (let [error-str (with-out-str (try (-> test-util/config-2 conf/load-config)
;;                                     (catch Throwable _ nil)))]
;;       (is (re-find #"Spec failed" error-str))
;;       (is (re-find #"should contain key:.{6}:nuzzle/base-url" error-str))))
;;   (testing "author registry author validation works"
;;     (let [config {:nuzzle/render-page nuzzle.test-util/render-page
;;                   :nuzzle/author-registry {:laura {:name "Laura Palmer"}}
;;                   [:blog :douglas-firs] {:nuzzle/title "Douglas Firs Smell Really Freakin Good"
;;                                          :nuzzle/author :agent-cooper}}
;;           error-str (with-out-str (try (conf/validate-config config) (catch Throwable _ nil)))]
;;       (is (re-find #"Spec failed" error-str))
;;       (is (re-find #"->\W*config\W*:nuzzle\/author-registry\W*keys\W*set" error-str)))))


(deftest create-get-pages
  (let [get-pages (-> test-util/twin-peaks-pages pages/load-pages pages/create-get-pages)]
    (is (= "About" (:nuzzle/title (get-pages [:about]))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"get-pages error"
                          (get-pages :bad-key)))))
