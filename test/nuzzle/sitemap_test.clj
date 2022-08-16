(ns nuzzle.sitemap-test
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.string :as str]
   [nuzzle.config :as conf]
   [nuzzle.generator :as gen]
   [nuzzle.sitemap :as sitemap]
   [nuzzle.util :as util]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/load-specified-config config-path {}))

(deftest create-sitemap
  (is (= (-> "test-resources/xml/empty-sitemap.xml" slurp str/trim)
         (sitemap/create-sitemap {} {})))
  (is (= (-> "test-resources/xml/config-1-sitemap.xml" slurp str/trim)
         (sitemap/create-sitemap (config) (gen/generate-rendered-site-index (config)))))
  (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns:a=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>https://foo.com/</loc></url><url><loc>https://foo.com/about/</loc><lastmod>2022-05-09T12:00:00Z</lastmod></url><url><loc>https://foo.com/blog-posts/foobar/</loc></url></urlset>"
         (sitemap/create-sitemap {:nuzzle/base-url "https://foo.com"
                                  [:about] {:nuzzle/updated (util/time-str->?inst "2022-05-09T12:00Z")}}
                                 {"/" []
                                  "/about/" []
                                  "/blog-posts/foobar/" []}))))

(comment (spit "test-resources/xml/empty-sitemap.xml" (sitemap/create-sitemap {} {})))
(comment (spit "test-resources/xml/config-1-sitemap.xml" (sitemap/create-sitemap (config) (gen/generate-rendered-site-index (config)))))
