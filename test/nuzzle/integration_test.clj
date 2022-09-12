(ns nuzzle.integration-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [cybermonday.core :as cm]
   [nuzzle.config :as conf]
   [nuzzle.content :as content]
   [nuzzle.log :as log]
   [nuzzle.util :as util]))

(defn render-page
  [{:nuzzle/keys [render-content]}]
  [:html [:body (render-content)]])

(declare ^:dynamic md->hiccup)

(defn read-ash-config []
  (try
    (let [ash-regex #"How I Caught Pikachu"
          ash-code (-> "README.md"
                       slurp
                       cm/parse-body
                       (util/find-hiccup-str ash-regex)
                       (str/replace "markdown/" "test-resources/markdown/")
                       (str/replace "md->hiccup" "nuzzle.content/md->hiccup"))
          config-start (.indexOf ash-code "(defn config")
          config-str (subs ash-code config-start)
          config-fn (binding [md->hiccup content/md->hiccup]
                      (-> config-str read-string eval))]
      (-> (config-fn)
          (assoc :nuzzle/render-page render-page)))
    (catch Throwable e
      (log/error "Could not read example config function in README.md")
      (throw e))))

(comment (read-ash-config))

(defn normalize-loaded-config
  "Calls the :nuzzle/render-content function in each page entry value so it's
  easier to test equality"
  [config]
  {:pre [(map? config)]}
  (let [trigger-render-content
        (fn [cval]
          (if-not (:nuzzle/render-content cval)
            cval
            (update cval :nuzzle/render-content #(%))))
        remove-get-config
        (fn [cval]
          (cond-> cval
            (map? cval) (dissoc :nuzzle/get-config)))]
    (-> config
        (update-vals trigger-render-content)
        (update-vals remove-get-config))))

(comment (-> (read-ash-config) normalize-loaded-config))

(deftest transform-config
  (let [config (-> (read-ash-config) conf/load-config)
        normalized-config (normalize-loaded-config config)]
    ;; Check that every page entry has a :nuzzle/get-config key
    (is (every? (fn [[ckey cval]] (or (not (vector? ckey)) (contains? cval :nuzzle/get-config)))
                config))
    (is (= normalized-config
           {:nuzzle/render-page render-page
            []
            {:nuzzle/title "Home"
             :nuzzle/index #{[:about] [:blog-posts]},
             :nuzzle/url []
             :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
            [:blog-posts :catching-pikachu]
            {:nuzzle/title "How I Caught Pikachu",
             :nuzzle/url [:blog-posts :catching-pikachu]
             :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
            [:about]
            {:nuzzle/title "About Ash"
             :nuzzle/url [:about]
             :nuzzle/render-content '([:h1 {:id "placeholder"} "Placeholder"])},
            [:blog-posts]
            {:nuzzle/index
             #{[:blog-posts :catching-pikachu]},
             :nuzzle/title "Blog Posts",
             :nuzzle/url [:blog-posts]
             :nuzzle/render-content '([:p {} "Hi I'm Ash and this is my blog!"])}}))))
