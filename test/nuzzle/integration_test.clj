(ns nuzzle.integration-test
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is]]
   [cybermonday.core :as cm]
   [nuzzle.config :as conf]
   [nuzzle.log :as log]
   [nuzzle.util :as util]))

(defn render-page
  [{:nuzzle/keys [render-content]}]
  [:html [:body (render-content)]])

(defn read-ash-config []
  (let [config-regex #"How I Caught Pikachu"
        config (->> "README.md"
                    slurp
                    cm/parse-body
                    (util/find-hiccup-str config-regex))]
    (when-not config
      (log/error "Could not read example config in README.md")
      (throw (ex-info (str "Could not locate example config with the regex"
                           (pr-str config-regex))
                      {})))
    (try (edn/read-string config)
      (catch Exception e
        (log/error "Could not read example config in README.md")
        (throw e)))))

(comment (read-ash-config))

(defn transform-ash-config
  [config]
  {:pre [(map? config) (set? (:site-data config))]}
  (let [update-content
        (fn [site-datum]
          (if-not (:content site-datum)
            site-datum
            (update site-datum :content #(str "test-resources/" %))))]
    (-> config
        (update :nuzzle/overlay-dir #(str "test-resources/" %))
        (update :nuzzle/render-page (constantly 'nuzzle.integration-test/render-page))
        (update :site-data #(map update-content %))
        (update :site-data set))))

(comment (-> (read-ash-config) transform-ash-config))

(defn create-ash-config-file
  [config]
  {:pre [(map? config) (set? (:site-data config))]}
  (let [tmp-file (fs/create-temp-file)
        tmp-file-path (-> tmp-file fs/canonicalize str)]
    (->> config
         pr-str
         (spit tmp-file-path))
    tmp-file-path))

(comment (-> (read-ash-config) transform-ash-config create-ash-config-file))

(defn normalize-loaded-config
  "Calls the :nuzzle/render-content function in each :site-data value so it's
  easier to test equality"
  [config]
  {:pre [(map? config) (map? (:site-data config))]}
  (let [trigger-render-content
        (fn [site-datum]
          (if-not (:nuzzle/render-content site-datum)
            site-datum
            (update site-datum :nuzzle/render-content #(%))))]
    (-> config
        (update :site-data #(update-vals % trigger-render-content)))))

(comment (-> (read-ash-config) transform-ash-config create-ash-config-file
             (conf/load-specified-config {}) normalize-loaded-config))

(deftest realize-site-data
  (is (= (-> (read-ash-config) transform-ash-config create-ash-config-file
             (conf/load-specified-config {}) normalize-loaded-config)
         {:nuzzle/publish-dir "out",
          :nuzzle/server-port 6899,
          :nuzzle/base-url "https://ashketchum.com",
          :nuzzle/overlay-dir "test-resources/overlay",
          :nuzzle/render-page render-page
          :site-data
          {[:blog-posts :defeating-misty]
           {:title "How I Defeated Misty with Pikachu",
            :content "test-resources/markdown/how-i-defeated-misty.md",
            :nuzzle/url "/blog-posts/defeating-misty/",
            :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
           []
           {:content "test-resources/markdown/homepage-introduction.md",
            :index #{[:about] [:blog-posts]},
            :nuzzle/url "/",
            :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
           [:blog-posts :catching-pikachu]
           {:title "How I Caught Pikachu",
            :content "test-resources/markdown/how-i-caught-pikachu.md",
            :nuzzle/url "/blog-posts/catching-pikachu/",
            :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
           [:about]
           {:content "test-resources/markdown/about-ash.md",
            :nuzzle/url "/about/",
            :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
           :crypto
           {:bitcoin "1GVY5eZvtc5bA6EFEGnpqJeHUC5YaV5dsb",
            :eth "0xc0ffee254729296a45a3885639AC7E10F9d54979",
            :nuzzle/render-content nil},
           [:blog-posts]
           {:index
            #{[:blog-posts :defeating-misty] [:blog-posts :catching-pikachu]},
            :title "Blog Posts",
            :nuzzle/url "/blog-posts/",
            :nuzzle/render-content nil}}})))
