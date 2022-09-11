(ns nuzzle.config-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.test-util :as test-util]))

;; (deftest validate-config
  ;; (testing "bad config fails with expound output"
  ;;   (let [error-str (with-out-str (try (-> test-util/config-2 conf/load-config)
  ;;                                   (catch Throwable _ nil)))]
  ;;     (is (re-find #"Spec failed" error-str))
  ;;     (is (re-find #"should contain key:.{6}:nuzzle/base-url" error-str))))
  ;; (testing "author registry author validation works"
  ;;   (let [config {:nuzzle/render-page nuzzle.test-util/render-page
  ;;                 :nuzzle/author-registry {:laura {:name "Laura Palmer"}}
  ;;                 [:blog :douglas-firs] {:nuzzle/title "Douglas Firs Smell Really Freakin Good"
  ;;                                        :nuzzle/author :agent-cooper}}
  ;;         error-str (with-out-str (try (conf/validate-config config) (catch Throwable _ nil)))]
  ;;     (is (re-find #"Spec failed" error-str))
  ;;     (is (re-find #"->\W*config\W*:nuzzle\/author-registry\W*keys\W*set" error-str)))))

(deftest create-tag-index-page-entries
  (is (= {[:tags :bar]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/title "#bar"}
          [:tags :baz]
          {:nuzzle/index #{[:blog :foo]},
           :nuzzle/title "#baz"}}
         (conf/create-tag-index-page-entries {[:blog :foo] {:nuzzle/tags #{:bar :baz}} [:about] {}})))
  (is (= {[:tags :nuzzle]
          {:nuzzle/index #{[:blog :nuzzle-rocks] [:blog :why-nuzzle]},
           :nuzzle/title "#nuzzle"}
          [:tags :colors]
          {:nuzzle/index #{[:blog :favorite-color]},
           :nuzzle/title "#colors"}}
         (conf/create-tag-index-page-entries test-util/config-1))))

(deftest create-hierarchical-index-page-entries
  (is (= {[:blog-posts]
          {:nuzzle/index #{[:blog-posts :foo] [:blog-posts :archive]}, :nuzzle/title "Blog Posts"},
          [:blog-posts :archive]
          {:nuzzle/index #{[:blog-posts :archive :baz]},
           :nuzzle/title "Archive"}
          [:projects]
          {:nuzzle/index #{[:projects :bee]}, :nuzzle/title "Projects"}
          []
          {:nuzzle/index #{[:blog-posts] [:projects]}
           :nuzzle/title "Home"}}
         (conf/create-hierarchical-index-page-entries {[:blog-posts :foo] {:nuzzle/title "Foo"}
                                  [:blog-posts :archive :baz] {:nuzzle/title "Baz"}
                                  [:projects :bee] {:nuzzle/title "Bee"}})))
  (is (= {[:blog]
          {:nuzzle/index
           #{[:blog :why-nuzzle] [:blog :favorite-color] [:blog :nuzzle-rocks]},
           :nuzzle/title "Blog"}
          []
          {:nuzzle/index #{[:about] [:blog]}
           :nuzzle/title "Home"}}
         (conf/create-hierarchical-index-page-entries test-util/config-1))))

(deftest create-get-config
  (let [get-config (-> test-util/config-1 conf/load-config conf/create-get-config)]
    (is (= "https://twitter/foobar" (get-config :meta :twitter)))
    (is (= [:about] (get-config [:about] :nuzzle/url)))
    (is (= #{[:blog :favorite-color] [:blog :nuzzle-rocks] [:blog :why-nuzzle]}
           (get-config [:blog] :nuzzle/index)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"get-config error: config key sequence \[:bad-key\] cannot be resolved"
                          (get-config :bad-key)))))
