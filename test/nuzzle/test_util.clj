(ns nuzzle.test-util
  (:require
   [nuzzle.core :refer [parse-md]]))

(def render-page (constantly [:h1 "test"]))

(def authors {:donna {:email "donnah@mail.com",
                      :name "Donna Hayward",
                      :url "https://donnahayward.com"},
              :josie {:name "Josie Packard"},
              :shelly {:email "shellyj@mail.com", :name "Shelly Johnson"}})

(def twin-peaks-pages
  {[]
   {:nuzzle/title "Home"
    :nuzzle/render-page render-page}

   [:about]
   {:nuzzle/updated #inst "2022-05-09T12:00Z",
    :nuzzle/render-content #(-> "test-resources/markdown/about.md" slurp parse-md),
    :nuzzle/render-page render-page
    :nuzzle/title "About"},

   [:blog :favorite-color]
   {:nuzzle/render-content #(-> "test-resources/markdown/favorite-color.md" slurp parse-md),
    :nuzzle/render-page render-page
    :nuzzle/updated #inst "2022-05-09T12:00Z"
    :nuzzle/feed? true,
    :nuzzle/author (authors :josie)
    :nuzzle/tags #{:colors},
    :nuzzle/title "What's My Favorite Color? It May Suprise You."},

   [:blog :nuzzle-rocks]
   {:nuzzle/render-content #(-> "test-resources/markdown/nuzzle-rocks.md" slurp parse-md),
    :nuzzle/render-page render-page
    :nuzzle/updated #inst "2022-05-09T12:00Z",
    :nuzzle/author (authors :shelly)
    :nuzzle/feed? true,
    :nuzzle/tags #{:nuzzle},
    :nuzzle/title "10 Reasons Why Nuzzle Rocks"},

   [:blog :why-nuzzle]
   {:nuzzle/render-content #(-> "test-resources/markdown/why-nuzzle.md" slurp parse-md),
    :nuzzle/render-page render-page
    :nuzzle/updated #inst "2022-05-09T12:00Z"
    :nuzzle/feed? true,
    :nuzzle/author (authors :donna)
    :nuzzle/tags #{:nuzzle},
    :nuzzle/title "Why I Made Nuzzle"}})
