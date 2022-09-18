<p align="center">
<img src="./assets/nuzzle2-with-text.svg" alt="Nuzzle logo" width="400">
</p>
<p align="center">
✨ A functional static site generator so smol you won't even notice it's there ✨
</p>
<div align="center">
  <img src="https://github.com/stelcodes/nuzzle/actions/workflows/unit-tests.yaml/badge.svg" alt="unit tests">
  <img src="https://github.com/stelcodes/nuzzle/actions/workflows/clj-kondo.yaml/badge.svg" alt="clj-kondo">
  <a href="https://clojars.org/codes.stel/nuzzle"><img src="https://img.shields.io/clojars/v/codes.stel/nuzzle.svg" alt="Clojars Project"></a>
</div>
<hr>

> **WARNING**: Nuzzle is in the alpha stage. Expect API breakages.

## What is Nuzzle?
Nuzzle is a static site generator packaged as a Clojure library. It requires an astonishingly small code footprint to use. Keep all your static site data and building instructions in a single Clojure file!

## Design Goals
- Easy to use for developers new to Clojure
- Highly flexible for advanced Clojure users
- Great development experience with excellent hot-reloadability story
- Semantically aligned with the [Atom feed spec](https://validator.w3.org/feed/docs/atom.html)
- Small code footprint! Your Clojure code interacting with Nuzzle can be extremely succinct such that you can keep it all in a single file and run Nuzzle functions via a Clojure CLI tool alias
- Users can define their own process for loading content so it can be of any type (Markdown, Org, HTML, etc) and come from anywhere (local files, headless CMS API calls, mix of both, etc)

## Feature List (Nuzzle users can...)
- Generate an Atom feed with embedded HTML content
- Generate a sitemap
- Generate tag index pages from tag information
- Define all markup in hiccup without ever worrying about converting it to HTML
- Retrieve all data for any page while creating markup
- Start dual website/nREPL servers without adding nREPL deps for creating incredible development feedback loop
- Inject a script based on [livejs](https://livejs.com/) with configurable refresh interval to automatically refresh browser view to see content/markup changes in realtime while developing
- Transform hiccup to do any customization imaginable including statically rendering syntax highlighted code blocks with [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma)

## Real World Example
Want to read some code already? Check out [this repo](https://github.com/stelcodes/dev-blog) which uses Nuzzle to build my personal developer blog deployed at [stel.codes](https://stel.codes).

![Pretty logging when running nuzzle.core/develop](https://user-images.githubusercontent.com/22163194/190880734-ff36a238-a00c-4431-a720-5308c07f57a1.png)

## Requirements
- Java >= 11
- Clojure >= 1.11.1

## Usage
Here's a minimal Nuzzle setup:

`deps.edn`
```clojure
{:aliases
 {:site {:extra-deps {codes.stel/nuzzle {:mvn/version "0.6.410"}
                      org.clojure/clojure {:mvn/version "1.11.1"}}
         :ns-default site}}}
```

`site.clj`
```clojure
(ns site
  (:require [nuzzle.core :as nuzz]))

;; Create a pages map
(defn pages []
  {[]
   {:nuzzle/title "Homepage"
    :nuzzle/render-page (fn [{:nuzzle/keys [title] :as _page}]
                          [:html
                           [:h1 title]
                           [:a {:href [:about]}]] "About")}
   [:about]
   {:nuzzle/title "About"
    :nuzzle/render-content #(-> "md/about.md" slurp nuzz/parse-md)
    :nuzzle/render-page (fn [{:nuzzle/keys [render-content title] :as _page}]
                          [:html
                           [:h1 title]
                           (render-content)])}})

;; Start static site server + nREPL server with nuzzle.core/develop
;; Pass the pages as a var to get full hot-reloading capabilities!
;; The returned value is a function that stops both servers.
(defn develop [_]
  (nuzz/develop #'pages))

;; Publish the static site to ./dist
(defn publish [_]
  (nuzz/publish pages))
```

Use Nuzzle by invoking the Clojure CLI tool alias from `deps.edn`:
```bash
# Work on site
clj -T:site develop
# Publish site
clj -T:site publish
```

## Pages Map
Nuzzle uses a map to model a static site where every key is a URL and every value is a map of details about the page. The pages map is validated by `clojure.spec`. You can find the [pages spec here](https://github.com/stelcodes/nuzzle/blob/main/src/nuzzle/schemas.clj).

If you're from Pallet town, your `site.clj` might look like this:
```clojure
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
        :nuzzle/render-content (md-content "content/homepage-introduction.md")
        :nuzzle/render-page render-page}

       [:blog-posts]
       {:nuzzle/title "Blog Posts"
        :nuzzle/render-content (md-content "content/blog-header.md")
        :nuzzle/render-page render-page}

       [:blog-posts :catching-pikachu]
       {:nuzzle/title "How I Caught Pikachu"
        :nuzzle/render-content (md-content "content/how-i-caught-pikachu.md")
        :nuzzle/render-page render-page
        :nuzzle/author ash
        :nuzzle/feed? true}

       [:blog-posts :defeating-misty]
       {:nuzzle/draft? true
        :nuzzle/title "How I Defeated Misty with Pikachu"
        :nuzzle/render-content (md-content "content/how-i-defeated-misty.md")
        :nuzzle/render-page render-page
        :nuzzle/author ash
        :nuzzle/feed? true}

       [:about]
       {:nuzzle/title "About Ash"
        :nuzzle/render-content (md-content "markdown/about-ash.md")
        :nuzzle/render-page render-page}}
  (nuzz/add-tag-pages render-page))

(defn develop [opts]
  (let [default-opts {:port 8080}]
    (nuzz/develop #'pages (merge default-opts opts))))

;; By default build this site with a sitemap and Atom feed
;; Overlay the directory containing the css/main.css file.

(defn publish [opts]
  (let [default-opts {:base-url "https://ashketchum.com"
                      :atom-feed {:title "Ash Ketchum's Blog"
                                  :subtitle "In a world we must defend"}
                      :overlay-dir "public"}]
    (nuzz/publish pages (merge default-opts opts)))
```

## Page Entries

Each page entry in the pages map represents a single page of the static site. Each entry must have a key that is a **vector of keywords** representing a relative URL. The key `[:blog-posts :catching-pikachu]` translates to the URL `"/blog-posts/catching-pikachu"` and will be rendered to disk as `<publish-dir>/blog-posts/catching-pikachu/index.html`. The associated value must be a map with data about the page. The keys of the page are based on the [Atom feed spec](https://validator.w3.org/feed/docs/atom.html).

```clojure
;; An entry for the page "/blog/the-best-thing-about-clojure/index.html"
[:blog :learning-clojure]

{;; A title for the web page
 ;; Required, no default
 :nuzzle/title "Learning Clojure"

 ;; A function that takes the page data and returns hiccup containing the whole HTML document
 ;; Must have one argument (the containing page map)
 ;; Used by nuzzle.core/serve, publish, and develop for creating each page of the static site
 ;; Required, no default
 :nuzzle/render-page (fn [{:nuzzle/keys [title render-content] :as _page}
                       [:html [:head [:title title]] [:h1 title] (render-content)])

 ;; A function that takes the page map and returns Hiccup containing the page's main content
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
                 :url "https://lucylambda.com"}}
```

## Automatically Added Keys to Page Entries
Nuzzle adds these keys to every page map:
- `:nuzzle/url`: The key of the page (the vector of keywords representing the URL) is added to the page map.
- `:nuzzle/index`: If a page does
- `:nuzzle/render-content`: If a page doesn't have a `render-content` function,  Nuzzle adds one which is `(constantly nil)`. This means it's always safe to call in a `render-page` function.
- `:nuzzle/get-pages`: A function that allows you to freely access your Nuzzle config from inside your `render-content` and `render-page` functions.

## Adding Tag Pages
The `:nuzzle/tag` page keys are used to generate tag pages by using the `nuzzle.core/add-tag-pages` function. This function requires a `:nuzzle/render-page` function for the tag pages.

```clojure
(-> {[:blog :foo]
     {:nuzzle/title "Foo"
      :nuzzle/render-page render-generic-page
      :nuzzle/tags #{:bar :baz}}
     [:about]
     {:nuzzle/title "About"
      :nuzzle/render-page render-about-page}}
    (nuzz/add-tag-pages render-tag-page)
==>
{[:blog :foo]
 {:nuzzle/title "Foo"
  :nuzzle/render-page render-generic-page
  :nuzzle/tags #{:bar :baz}}
 [:about]
 {:nuzzle/title "About"
  :nuzzle/render-page render-about-page}
 [:tags :bar]
 {:nuzzle/title "Tag bar"
  :nuzzle/render-page render-tag-page
  :nuzzle/index #{[:blog :foo]}
 [:tags :baz]
 {:nuzzle/title "Tag baz"
  :nuzzle/render-page render-tag-page
  :nuzzle/index #{[:blog :foo]}}}
```

## Creating a `render-page` Function
Each function under the `:nuzzle/render-page` key must turn that page map into Hiccup. The function must take one parameter (a page entry map). It can return a vector of Hiccup (more flexible) or a string of HTML (wrapped with `nuzzle.hiccup/raw-html`)).

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `nuzzle.hiccup/raw-html` wrapper function like so: `(raw-html "<h1>Title</h1>")`.

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

## Using the `get-pages` Function
With many static site generators, accessing global data inside markup templates can be *painful*. Since Nuzzle is just playing with data and functions, this problem becomes much easier to solve.

Instead of complicating your page rendering functions with multiple required arguments, Nuzzle adds a function to each page entry map under the key `:nuzzle/get-pages`. This function can access the whole pages map, making referencing other pages a breeze.

```clojure
;; Get a list of every page
(get-pages)
;; Get a single page
(get-pages [:blog-posts])
;; Get a list of all the direct children of a page
(get-pages [:blog-posts] :children? true)
```

The `get-pages` function will always return pages which also have the `:nuzzle/get-pages` key attached. This naturally lends itself to a convention where most Hiccup-generating functions can accept a page entry map as its first or only argument while still being able to access any data in your whole site if need be.
