;; An entry for the page "/blog/the-best-thing-about-clojure/index.html"
[:blog :learning-clojure]

;; A title for the web page
;; Required, no default
{:nuzzle/title "Learning Clojure"

 ;; A function returns hiccup containing the whole HTML document
 ;; Must have one argument (the containing page map)
 ;; Used by nuzzle.core/serve, publish, and develop for creating each page of the static site
 ;; Required, no default
 :nuzzle/render-page  (fn [{:nuzzle/keys [title render-content] :as _page}]
                        [:html [:head [:title title]] [:h1 title] (render-content)])

 ;; A function that returns hiccup containing the page's main content
 ;; Must have zero or one argument (the containing page map)
 ;; Available to user's corresponding :nuzzle/render-page function
 ;; Used by nuzzle.core/publish for creating Atom feed (for embedding HTML content directly into feed)
 ;; Optional, defaults to (constantly nil)
 :nuzzle/render-content (fn [] [:p "The first step to learning Clojure is pressing the ( key."])

 ;; A boolean indicating whether this page is a draft or not
 ;; Used by nuzzle.core/serve, develop, and publish via :remove-drafts? keyword argument to remove unfinished page entries
 ;; Optional, defaults to nil
 :nuzzle/draft? false

 ;; A boolean indicating whether the page should be included in the optional Atom feed when publishing
 ;; Used by nuzzle.core/publish for creating Atom feed
 ;; Optional, defaults to nil
 :nuzzle/feed? true

 ;; A set of keywords where each keyword represents a tag name
 ;; Used by nuzzle.core/add-tag-pages for assoc-ing tag index pages
 ;; Optional, defaults to nil
 :nuzzle/tags #{:clojure}

 ;; Either a set of vector URLs (vectors of keywords), or the keyword :children
 ;; Describes what pages should be linked to from this page
 ;; When set to :children, Nuzzle will replace it with set of URLs of all the pages directly "beneath" this page
 ;; This key is always present in tag pages when enabled with the :tag-pages keyword argument to nuzzle.core/serve, develop, publish
 ;; Optional, defaults to nil
 :nuzzle/index :children

 ;; An inst representing when the page was last updated
 ;; Used by nuzzle.core/publish for creating sitemap and Atom feed
 ;; Optional, defaults to nil
 :nuzzle/updated #inst "2022-09-16T12:00:00Z"

 ;; A string summary of the page content
 ;; Used by nuzzle.core/publish for creating Atom feed
 ;; Optional, defaults to nil
 :nuzzle/summary "An in-depth guide to learning Clojure."

 ;; A map representing the author of the page.
 ;; Used by nuzzle.core/publish for creating Atom feed
 ;; Optional, defaults to nil
 :nuzzle/author {;; A string name, required per the Atom spec
                 :name "Lucy Lambda"
                 ;; A string email, optional
                 :email "lucylamda@email.com"
                 ;; A string URL for the author's homepage, optional
                 :url "https://lucylambda.com"}

 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ;; Automatically added keys (you don't have to add these manually)

 ;; The URL vector of keywords is copied from the key into the map itself by Nuzzle
 ;; Always present
 :nuzzle/url [:blog-posts :using-nuzzle]

 ;; A function that can access the whole page map, takes zero or one arguments
 ;; Zero arguments returns a list of all pages
 ;; One argument returns a page map with given vector URL
 ;; One argument with options map argument {:children true} returns a list of all children pages for given vector URL
 ;; Nuzzle adds this so your render-page function doesn't have to accept more than one argument
 ;; Always present
 :nuzzle/get-pages (fn get-pages ...)}
