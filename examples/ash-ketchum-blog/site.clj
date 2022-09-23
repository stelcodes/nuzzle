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
                :let [{:nuzzle/keys [title url] :as _indexed-page} (get-pages url)]]
            ;; The url is a vector of keywords, but Nuzzle will convert them
            ;; into relative URL strings when used for an :href value
            [:li [:a {:href url} title]])])
   (render-content)]])

;; Here we make a function that creates a render-content function that turns markdown files into hiccup
(defn md-content [md-path]
  #(-> md-path slurp nuzz/parse-md))

;; Here we define an author for our Atom feed
(def ash {:name "Ash Ketchum"
          :email "ashketchum@fastmail.com"})

(defn pages []
  {[]
   {:nuzzle/title "Home"
    :nuzzle/render-content (md-content "content/intro.md")
    :nuzzle/render-page render-page
    :nuzzle/index :children}

   [:about]
   {:nuzzle/title "About Me"
    :nuzzle/render-content (md-content "content/about.md")
    :nuzzle/render-page render-page}

   [:blog-posts]
   {:nuzzle/title "Blog Posts"
    :nuzzle/render-page render-page
    :nuzzle/index :children}

   [:blog-posts :catching-pikachu]
   {:nuzzle/title "I Caught a Pikachu!"
    :nuzzle/render-content (md-content "content/caught-pikachu.md")
    :nuzzle/render-page render-page
    :nuzzle/tags #{:pokedex}
    :nuzzle/author ash
    :nuzzle/feed? true}

   [:blog-posts :defeating-misty]
   {:nuzzle/draft? true
    :nuzzle/title "Misty's Pokemon Got Wrecked by Pikachu"
    :nuzzle/render-content (md-content "content/defeated-misty.md")
    :nuzzle/render-page render-page
    :nuzzle/tags #{:gym-battle}
    :nuzzle/author ash
    :nuzzle/feed? true}

   [:tags]
   {:nuzzle/title "Tags"
    :nuzzle/render-page render-page
    :nuzzle/index :children}})

(defn develop [_]
  (nuzz/develop #'pages {:remove-drafts false
                         :tag-pages {:render-page render-page}
                         :overlay-dir "public"}))

;; By default build this site with a sitemap and Atom feed
;; Overlay the directory containing the css/main.css file.

(defn publish [_]
  (nuzz/publish pages {:base-url "https://ashketchum.com"
                       :tag-pages {:render-page render-page}
                       :atom-feed {:title "Ash Ketchum's Blog"
                                   :subtitle "In a world we must defend"}
                       :overlay-dir "public"}))
