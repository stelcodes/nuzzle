<p align="center">
<img src="./assets/nuzzle2-with-text.svg" width="400">
</p>
<p align="center">
✨ A data-oriented, REPL-driven static site generator for Clojure ✨
</p>
<hr>

> **WARNING**: Nuzzle is in the alpha stage. Expect API breakages.

## Design Goals
With Nuzzle you can...
- create beautiful static websites
- describe the entire website structure declaratively inside an EDN map
- plug in a single function that produces Hiccup to render every webpage
- retrieve all website information while inside that function
- write content using markup languages (markdown, html)
- create an RSS feed
- tag webpages
- create subdirectory and tag index web pages
- set up a REPL-driven rapid feedback loop with built-in hot-reloading web server

## Real World Example
Want to read some code? Check out [this repo](https://github.com/stelcodes/dev-blog) which uses Nuzzle to build [this website](https://stel.codes).

## Nuzzle's API
All of Nuzzle's functionality is conveniently wrapped up with just three functions in the `codes.stel.nuzzle.api` namespace:
- `inspect`: Returns a more fleshed-out version of the site data with Nuzzle's additions.
- `start-server`: Starts a web server (http-kit) with a live preview the website. Builds each page from scratch upon each request. Use in a REPL session for hot-reloading.
- `export`: Exports the static site to disk.

To keep things simple, all three functions have the exact same signature. They all accept a single map with the following keys:
```clojure
:site-data      ; A path to an EDN file containing data about the website. Required.
:render-page    ; A function responsible for creating Hiccup for every webpage listed in the site-data. Required.
:static-dir     ; A path to a directory that contains the static assets for the site. Defaults to nil (no static assets).
:target-dir     ; A path to a directory to put the exported site into. Defaults to `dist`.
:rss-opts       ; A map with RSS feed options. Defaults to nil (no RSS feed).
:remove-drafts? ; A boolean that indicates whether pages marked as a draft should be removed. Defaults to false.
:dev-port       ; A port number for the development server to listen on. Defaults to 5868.
```

## Site Data
Your `site-data` EDN file defines all the webpages in the website, plus any extra information you may need. It should be a vector of maps. Each map must have the key `:id`. The value of `:id` will descibes what kind of data that map holds.

If the `:id` is a **vector of keywords**, it represents a typical **webpage**. The `:id` `[:blog-posts :using-clojure]` translates to the URI `"/blog-posts/using-clojure"` and will be rendered to disk as `<target-dir>/blog-posts/using-clojure/index.html`. We'll refer to these as *webpage maps*.

If the `:id` is a singular **keyword**, the map just contains extra information about the site. It has no effect on the website structure. It can easily be retrieved inside your `render-page` function later on.

Here's an annotated example:
```clojure
[
  ;; The homepage (required)
  {:id []} ; <- This represents the URI "/"

  {:id [:about] ; <- This represents the URI "/about"
   :title "About"} ; <- Add a title if you'd like

  {:id [:blog-posts :using-clojure]
   :title "Using Clojure"
   ;; The special :content key points to a markup file
   :content "markdown/using-clojure.md"
   ;; The special :tags key tells Nuzzle about webpage tags
   :tags [:clojure]}

  {:id [:blog-posts :learning-rust]
   :title "How I Got Started Learning Rust"
   :content "markdown/learning-rust.md"
   :tags [:rust]
   ;; The special :draft? key tells Nuzzle which webpages are drafts
   :draft? true
   ;; The special :rss key tells Nuzzle to include the webpage in the RSS XML file
   :rss true}

  {:id [:blog-posts :clojure-on-fedora]
   :title "How to Install Clojure on Fedora"
   :content "markdown/clojure-on-fedora.md"
   :tags [:linux :clojure]
   ;; Webpage maps are open, you can include any other data you like
   :foobar "baz"}

  ;; Extra information not particular to any webpage
  {:id :social
   :twitter "https://twitter.com/clojurerulez"} ; <- This will be easy to retrieve later
]
```

### Special Keys in Webpage Maps
Nuzzle recognizes some special keys in webpage maps which have side-effects:
- `:content`: A path to a file that contains markup. Nuzzle decided what kind of markup it is based on the filename suffix. Supported filetypes are HTML: `.html` and Markdown: `.md`, `.markdown`. Nuzzle will add a function that renders the markup under the key `:render-content`.
- `:tags`: A vector of keywords. For each unique tag in the website, Nuzzle will add a tag index page to the `site-data` map under the key `[:tags <tag>]`.
- `:draft?`: A boolean indicating whether this page is a draft or not. When true and `:remove-drafts?` from the top-level config is also true, this webpage will not be passed to your `render-page` function.
- `:rss`: A boolean indicating whether the webpage should be included in the optional RSS feed.

## How Nuzzle Transforms the Site Data
You can think of Nuzzle's core functionality as a data pipeline. Nuzzle takes your site data, applies some transformations, and then sends each webpage map to your page rendering function.

Nuzzle's process can be visualized like so:
```
   ┌───────────┐           ┌───────────┐               ┌──────────────┐
   │           │           │           │               │              │
   │ site data │ ────┬───► │ realized  │ ─────┬─────►  │  Hiccup that │
   │   from    │     │     │ site data │      │        │ gets rendered│
   │ EDN file  │     │     │           │      │        │ and exported │
   │           │           │           │               │   to disk    │
   └───────────┘  Nuzzle   └───────────┘  render-page  │              │
              transformations              function    └──────────────┘
```

### Adding Keys to Webpage Maps
Nuzzle adds these keys to every webpage map:
- `:uri`: the path of the webpage from the website's root, (ex `"/blog-posts/learning-clojure/"`)
- `:render-content`: A function that renders the page's markup if `:content` key is present, otherwise returns `nil`.
- `:id->info`: A function that takes any `id` from the site data and returns the corresponding map. Useful in your page rendering function.

### Adding Index Pages
Often you'll want to create index pages in static sites which serve as a page that links to other pages which share a common trait. For example, if you have webpages like `"/blog-posts/foo"` and `"/blog-posts/bar"`, you may want a webpage at `"/blog-posts"` that links to these pages. Let's call these subdirectory index pages. Another common pattern is associating tags with webpages and having tag index pages like `"/tags/clojure"` which links to all your webpages about Clojure. Let's call these tag index pages. Nuzzle adds both subdirectory and tag index pages automatically for all subdirectories and tags present in your site data. It's up to you whether to render them in your page rendering function.

What makes these index webpage maps special is that they have an `:index` key with a value that is a vector of webpage `id`s. For example, the subdirectory index webpage map for the above example would have a key `:index` with a value of `[[:blog-posts :foo] [:blog-posts :bar]]`. Inside of your render function, you will be able to retrieve data for a webpage given a webpage id with the function `id->info`, and this way you can easily find the title and URIs of your indexed pages.

It's worth noting that you can define index pages manually in your site data, and Nuzzle will simply append the `:index` key/value pair. You can use this to add some markup to your index pages from a markdown file by including a `:content` key.

## Creating a Page Rendering Function
All webpages are transformed into Hiccup by a single function supplied by the user in the top-level config map under the key `:render-page`. This function takes a single argument (a webpage map) and returns a vector of Hiccup.

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `codes.stel.nuzzle.hiccup/raw` wrapper function like so: `(raw "<h1>Title</h1>")`.

Here's an example of a page rendering function called `render-page`:
```clojure
(defn layout [title & body]
  [:html [:head [:title title]]
   (into [:body] body)])

(defn render-page [{:keys [id title render-content] :as page}]
  (cond
    ;; Decide what page should look like based on the id
    (= [] id) (layout title [:h1 "Home Page"] [:a {:href "/about"} "About"])
    (= [:about] id) (layout title [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
    :else (layout title [:h1 title] (render-content)]))
```

Here we see the `:render-content` function being used. It will always be present in the webpage map and it will either return `nil` or some HTML wrapped in the `codes.stel.nuzzle.hiccup/raw` wrapper.

### More data with `id->info`
Along with `:render-content`, Nuzzle attaches another key to every webpage map: `:id->info`. It's value is a special function that accepts an `id` like `[:about]` and returns a webpage map corresponding to that `id`. If the `id` is not found, it will throw an `Exception`. `id->info` is all about convenience. We can use it to create index pages among many other things:

```clojure
(defn layout [title & body]
  [:html [:head [:title title]]
   (into [:body] body)])

(defn render-index [{:keys [title index id->info]}]
  (layout title
          [:h1 (str "Index page for " title)]
          (for [id index
                :let [page (id->info id)]]
            [:a {:href (:uri page)} (:title page)]))

(defn render-page [{:keys [id title render-content id->info] :as page}]
  (cond
    (= [] id) (layout title [:h1 "Home Page"]
                            [:a {:href (:uri (id->info [:about]))} "About"]
                            [:a {:href (id->info :social) "Twitter"])
    (= [:about] id) (layout title [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
    (contains? page :index) (render-index page)
    :else (layout title [:h1 title] (render-content)]))
```

A neat thing about `id->info` is that every webpage map returned will also have an `id->info` function attached to it, so any function that accepts a webpage map can rely on it being inside that map.

## Generating an RSS feed
Nuzzle comes with support for generating an RSS feed. (TODO)
