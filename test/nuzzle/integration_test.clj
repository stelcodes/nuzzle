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
  {:pre [(map? config)]}
  (let [update-content
        (fn [cval]
          (if-not (:nuzzle/content cval)
            cval
            (update cval :nuzzle/content #(str "test-resources/" %))))]
    (-> config
      ;; (update :nuzzle/overlay-dir #(str "test-resources/" %))
      (update :nuzzle/render-page (constantly 'nuzzle.integration-test/render-page))
      (update-vals update-content))))

(comment (-> (read-ash-config) transform-ash-config))

(defn create-ash-config-file
  [config]
  {:pre [(map? config)]}
  (let [tmp-file (fs/create-temp-file)
        tmp-file-path (-> tmp-file fs/canonicalize str)]
    (->> config
         pr-str
         (spit tmp-file-path))
    tmp-file-path))

(comment (-> (read-ash-config) transform-ash-config create-ash-config-file))

(defn normalize-loaded-config
  "Calls the :nuzzle/render-content function in each page entry value so it's
  easier to test equality"
  [config]
  {:pre [(map? config)]}
  (let [trigger-render-content
        (fn [cval]
          (if-not (:nuzzle/render-content cval)
            cval
            (update cval :nuzzle/render-content #(%))))]
    (-> config
        (update-vals trigger-render-content))))

(comment (-> (read-ash-config) transform-ash-config create-ash-config-file
             (conf/load-specified-config) normalize-loaded-config))

(deftest realize-config
  (is (= (-> (read-ash-config) transform-ash-config create-ash-config-file
             (conf/load-specified-config) normalize-loaded-config)
         {:nuzzle/publish-dir "out",
          :nuzzle/server-port 6899,
          :nuzzle/base-url "https://ashketchum.com",
          :nuzzle/render-page render-page
          []
          {:nuzzle/title "Home"
           :nuzzle/content "test-resources/markdown/homepage-introduction.md",
           :nuzzle/index #{[:about] [:blog-posts]},
           :nuzzle/url "/",
           :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
          [:blog-posts :catching-pikachu]
          {:nuzzle/title "How I Caught Pikachu",
           :nuzzle/content "test-resources/markdown/how-i-caught-pikachu.md",
           :nuzzle/url "/blog-posts/catching-pikachu/",
           :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
          [:about]
          {:nuzzle/title "About Ash"
           :nuzzle/content "test-resources/markdown/about-ash.md",
           :nuzzle/url "/about/",
           :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
          [:blog-posts]
          {:nuzzle/index
           #{[:blog-posts :catching-pikachu]},
           :nuzzle/title "Blog Posts",
           :nuzzle/url "/blog-posts/",
           :nuzzle/content "test-resources/markdown/blog-header.md"
           :nuzzle/render-content '([:p {} "Hi I'm Ash and this is my blog!"])}})))
