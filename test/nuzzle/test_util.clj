(ns nuzzle.test-util)

(def render-page (constantly [:h1 "test"]))

(def config-1
  {:nuzzle/publish-dir "/tmp/nuzzle-test-out",
   :nuzzle/base-url "https://foobar.com"
   :nuzzle/sitemap? true
   :nuzzle/build-drafts? true,
   :nuzzle/render-page render-page,
   :nuzzle/author-registry {:donna {:email "donnah@mail.com",
                                    :name "Donna Hayward",
                                    :url "https://donnahayward.com"},
                            :josie {:name "Josie Packard"},
                            :shelly {:email "shellyj@mail.com", :name "Shelly Johnson"}}
   :nuzzle/atom-feed {:author :donna,
                      :subtitle "Rants about foo and thoughts about bar",
                      :title "Foo's blog"}
   :meta {:twitter "https://twitter/foobar"},
   [] {:nuzzle/title "Home"},
   [:about] {:nuzzle/updated "2022-05-09T12:00Z",
             :nuzzle/content "test-resources/markdown/about.md",
             :nuzzle/title "About"},
   [:blog :favorite-color] {:nuzzle/content "test-resources/markdown/favorite-color.md",
                            :nuzzle/updated "2022-05-09T12:00Z"
                            :nuzzle/feed? true,
                            :nuzzle/author :josie
                            :nuzzle/tags #{:colors},
                            :nuzzle/title "What's My Favorite Color? It May Suprise You."},
   [:blog :nuzzle-rocks] {:nuzzle/content "test-resources/markdown/nuzzle-rocks.md",
                          :nuzzle/updated "2022-05-09T12:00Z",
                          :nuzzle/author :shelly
                          :nuzzle/feed? true,
                          :nuzzle/tags #{:nuzzle},
                          :nuzzle/title "10 Reasons Why Nuzzle Rocks"},
   [:blog :why-nuzzle] {:nuzzle/content "test-resources/markdown/why-nuzzle.md",
                        :nuzzle/updated "2022-05-09T12:00Z"
                        :nuzzle/feed? true,
                        :nuzzle/author :donna
                        :nuzzle/tags #{:nuzzle},
                        :nuzzle/title "Why I Made Nuzzle"}})


(def config-2
  {:nuzzle/build-drafts? true
   :nuzzle/render-page render-page
   :nuzzle/sitemap? true
   :nuzzle/publish-dir "/tmp/nuzzle-test-out"

   [] {:nuzzle/title "Home"}

   [:blog :nuzzle-rocks]
   {:nuzzle/title "10 Reasons Why Nuzzle Rocks"
    :nuzzle/content "test-resources/markdown/nuzzle-rocks.md"
    :nuzzle/updated "2022-05-09"
    :nuzzle/tags #{:nuzzle}}

   [:blog :why-nuzzle]
   {:nuzzle/title "Why I Made Nuzzle"
    :nuzzle/content "test-resources/markdown/why-nuzzle.md"
    :nuzzle/tags #{:nuzzle}}

   [:blog :favorite-color]
   {:nuzzle/title "What's My Favorite Color? It May Suprise You."
    :nuzzle/content "test-resources/markdown/favorite-color.md"
    :nuzzle/tags #{:colors}}

   [:about]
   {:nuzzle/title "About"
    :nuzzle/content "test-resources/markdown/about.md"}

   :meta
   {:twitter "https://twitter/foobar"}})

