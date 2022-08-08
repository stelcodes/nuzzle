(ns nuzzle.sitemap-test
  (:require
   [clojure.test :refer [deftest is]]
   [nuzzle.config :as conf]
   [nuzzle.generator :as gen]
   [nuzzle.sitemap :as sitemap]))

(def config-path "test-resources/edn/config-1.edn")

(defn config [] (conf/load-specified-config config-path {}))

(deftest create-sitemap
  (is (= (slurp "test-resources/xml/empty-sitemap.xml")
         (sitemap/create-sitemap {} {})))
  (is (= (slurp "test-resources/xml/config-1-sitemap.xml")
         (sitemap/create-sitemap (config) (gen/generate-rendered-site-index (config)))))
  (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns:a=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>https://foo.com/</loc></url><url><loc>https://foo.com/about/</loc><lastmod>2022-05-09</lastmod></url><url><loc>https://foo.com/blog-posts/foobar/</loc></url></urlset>"
         (sitemap/create-sitemap {:nuzzle/base-url "https://foo.com"
                                  [:about] {:modified (java.time.LocalDate/parse "2022-05-09")}}
                                 {"/" []
                                  "/about/" []
                                  "/blog-posts/foobar/" []}))))

(comment (spit "test-resources/xml/empty-sitemap.xml" (sitemap/create-sitemap {} {})))
(comment (spit "test-resources/xml/config-1-sitemap.xml" (sitemap/create-sitemap (config) (gen/generate-rendered-site-index (config)))))
