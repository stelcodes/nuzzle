(ns site
  (:require
   [nuzzle.core :as nuzz]))

(defn render-page [{:nuzzle/keys [title index render-content get-pages] :as _page}]
 [:html
  [:head [:title title]
         ;; Add link to CSS file /css/main.css (must be in overlay directory)
         [:link {:href "/css/main.css" :rel "stylesheet"}]]
  [:body
   [:h1 title]
   (when index
     [:ul (for [url index
                :let [{:nuzzle/keys [title url]} (get-pages url)]]
            ;; The url is a vector, but Nuzzle will convert them
            ;; into strings when used for an :href value
            [:li title [:a {:href url}]])])
   (render-content)]])

;; Here we make a function to create a render-content function for markdown files
(defn md-content [md-path]
  #(-> md-path slurp nuzz/parse-md))

;; Here we define an author for our Atom feed
(def ash {:name "Ash Ketchum"
          :email "ashketchum@fastmail.com"})

(defn pages []
  (-> {[]
       {:nuzzle/title "Home"
        :nuzzle/render-content (md-content "content/intro.md")
        :nuzzle/render-page render-page}

       [:blog-posts]
       {:nuzzle/title "Blog Posts"
        :nuzzle/render-page render-page}

       [:blog-posts :catching-pikachu]
       {:nuzzle/title "I Caught a Pikachu!"
        :nuzzle/render-content (md-content "content/caught-pikachu.md")
        :nuzzle/render-page render-page
        :nuzzle/author ash
        :nuzzle/feed? true}

       [:blog-posts :defeating-misty]
       {:nuzzle/draft? true
        :nuzzle/title "Misty's Pokemon Got Wrecked by Pikachu"
        :nuzzle/render-content (md-content "content/defeated-misty.md")
        :nuzzle/render-page render-page
        :nuzzle/author ash
        :nuzzle/feed? true}

       [:about]
       {:nuzzle/title "About Me"
        :nuzzle/render-content (md-content "content/about.md")
        :nuzzle/render-page render-page}}
      (nuzz/add-tag-pages render-page)))

(defn develop [_]
  (nuzz/develop #'pages :port 8080))

;; By default build this site with a sitemap and Atom feed
;; Overlay the directory containing the css/main.css file.

(defn publish [_]
  (nuzz/publish pages {:base-url "https://ashketchum.com"
                       :atom-feed {:title "Ash Ketchum's Blog"
                                   :subtitle "In a world we must defend"}
                       :overlay-dir "public"}))
