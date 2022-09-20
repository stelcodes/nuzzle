(ns nuzzle.publish-test
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [nuzzle.pages :as pages]
   [nuzzle.publish :as publish]
   [nuzzle.test-util :as test-util]
   [nuzzle.util :as util]))

(deftest create-sitemap
  (is (= (-> "test-resources/xml/empty-sitemap.xml" slurp str/trim)
         (publish/create-sitemap {} {}))))

(deftest publish-site
  (let [temp-site-dir (str (fs/create-temp-dir))
        reference-dir (str (fs/path "test-resources/sites/twin-peaks"))
        reference-snapshot (util/create-dir-snapshot reference-dir)
        pages (-> test-util/twin-peaks-pages pages/load-pages)
        atom-feed {:title "Foo's blog"
                   :author (test-util/authors :donna)
                   :subtitle "Rants about foo and thoughts about bar"}]
    (with-redefs [nuzzle.util/now-trunc-sec (constantly "2022-09-15T12:00Z")]
      (publish/publish-site pages :overlay-dir "test-resources/public"
                            :base-url "https://foobar.com"
                            :tag-pages {:render-page test-util/render-page}
                            :publish-dir temp-site-dir
                            :atom-feed atom-feed))
    (let [site-snapshot (util/create-dir-snapshot temp-site-dir)
          diff (util/create-dir-diff reference-snapshot site-snapshot)]
      (when-not (every? empty? (vals diff))
        (-> (util/safe-sh "diff" "-r" reference-dir temp-site-dir) :out println))
      (is (every? empty? (vals diff))))))
