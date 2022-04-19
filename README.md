<p align="center">
<img src="./assets/nuzzle2-with-text.svg" width="400">
</p>
<p align="center">
✨ A data-oriented, REPL-driven static site generator for Clojure ✨
</p>
<hr>

> **WARNING**: Nuzzle is in the alpha stage. Expect API breakages.

## What is Nuzzle?
Nuzzle is a static site generator that's for people who love:
- Data-oriented functional programming ✨
- REPL-driven workflows 🔁
- The awesome combination of Clojure and Hiccup 💞
- Simplicity 🌷

Nuzzle is a Clojure library, but you could also think about it as a micro-framework. It's goal is to define a simple yet powerful process for turning data and functions into a static site. Nuzzle aims to be the only dependency that your static site project needs. It also aims to provide a rich REPL experience that can leverage the power of nREPL clients like Cider and Conjure for extremely fast feedback loops while experimenting with your site appearance and content.

**With Nuzzle you can...**
- Create beautiful static websites
- Manage all website data inside an EDN file
- Plug in a single function that produces Hiccup for every webpage
- Retrieve all website data while inside that function
- Keep content in markup files (Markdown, HTML)
- Utilize a built-in, REPL-driven, hot-reloading web server
- Tag webpages
- Create subdirectory and tag index webpages
- Create an RSS feed

## Real World Example
Want to read some code already? Check out [this repo](https://github.com/stelcodes/dev-blog) which uses Nuzzle to build my personal developer blog deployed at [stel.codes](https://stel.codes).

## Nuzzle's API
All of Nuzzle's functionality is conveniently wrapped up with just three functions in the `codes.stel.nuzzle.api` namespace:
- `inspect`: Returns a more fleshed-out version of the site data with Nuzzle's additions.
- `start-server`: Starts a web server (http-kit) with a live preview the website. Builds each webpage from scratch upon each request. Use in a REPL session for hot-reloading.
- `export`: Exports the static site to disk.

To keep things simple, all three functions have the exact same signature. They all accept a single map with the following keys:
```clojure
:site-data      ; A path to an EDN file containing data about the website. Required.
:render-webpage ; A function responsible for creating Hiccup for every webpage listed in the site-data. Required.
:static-dir     ; A path to a directory that contains the static assets for the site. Defaults to nil (no static assets).
:target-dir     ; A path to a directory to put the exported site into. Defaults to `dist`.
:rss-opts       ; A map with RSS feed options. Defaults to nil (no RSS feed).
:remove-drafts? ; A boolean that indicates whether webpages marked as a draft should be removed. Defaults to false.
:dev-port       ; A port number for the development server to listen on. Defaults to 5868.
```

## Site Data
Your `site-data` EDN file defines all the webpages in the website, plus any extra information you may need. It is expected to be a vector of maps. Each map has just one required key: `:id`. The value of `:id` will descibes what kind of data that map holds.

If the `:id` is a **vector of keywords**, it represents a typical **webpage**. The `:id` `[:blog-posts :using-clojure]` translates to the URI `"/blog-posts/using-clojure"` and will be rendered to disk as `<target-dir>/blog-posts/using-clojure/index.html`. We'll refer to these as *webpage maps*.

If the `:id` is a singular **keyword**, the map just contains extra information about the site. It has no effect on the website structure. It can easily be retrieved inside your `render-webpage` function later on. We'll refer to these as *metadata maps*.

Here's an annotated example:
```clojure
[
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Webpage Maps

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
   ;; Webpage maps are open, you can include any data you like
   :foobar "baz"}

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Metadata Maps

  ;; Extra information not particular to any webpage
  {:id :social
   :twitter "https://twitter.com/clojurerulez"} ; <- This will be easy to retrieve later
]
```

### Special Keys in Webpage Maps
Nuzzle recognizes some special keys in webpage maps which have side-effects:
- `:content`: A path to a file that contains markup. Nuzzle decided what kind of markup it is based on the filename suffix. Supported filetypes are HTML (`.html`) and Markdown (`.md`, `.markdown`).
- `:tags`: A vector of keywords where each keyword is a tag name.
- `:draft?`: A boolean indicating whether this webpage is a draft or not.
- `:rss`: A boolean indicating whether the webpage should be included in the optional RSS feed.

## How Nuzzle Transforms the Site Data
You can think of Nuzzle's core functionality as a data pipeline. Nuzzle takes your site data, applies some transformations, and then sends each webpage map to your webpage rendering function.

Nuzzle's process can be visualized like so:
```
   ┌───────────┐           ┌───────────┐               ┌──────────────┐
   │           │           │           │               │              │
   │ Site data │ ────┬───► │ Realized  │ ─────┬─────►  │  Hiccup that │
   │   from    │     │     │ site data │      │        │ is converted │
   │ EDN file  │     │     │           │      │        │ to html and  │
   │           │           │           │               │   exported   │
   └───────────┘  Nuzzle   └───────────┘render-webpage │              │
              transformations              function    └──────────────┘
```

A key part of this process is the first arrow: Nuzzle's transformations. Nuzzle refers to this as **realizing** your site data. The realized site data is a vector of maps just like the original, but with extra webpage maps and extra keys in the webpage maps from your EDN file.

### Adding Keys to Webpage Maps
Nuzzle adds these keys to every webpage map:
- `:uri`: the path of the webpage from the website's root, (ex `"/blog-posts/learning-clojure/"`).
- `:render-content`: A function that renders the webpage's markup if `:content` key is present, otherwise returns `nil`.
- `:id->info`: A function that takes any `id` from the realized site data and returns the corresponding map. Very useful in your webpage rendering function.

> Nuzzle does not modify metadata maps in any way.

### Adding Index Pages
Often you'll want to create index webpages in static sites which serve as a webpage that links to other webpages which share a common trait. For example, if you have webpages like `"/blog-posts/foo"` and `"/blog-posts/bar"`, you may want a webpage at `"/blog-posts"` that links to `"/blog-posts/foo"` and `"/blog-posts/bar"`. Let's call these *subdirectory index webpages*. Another common pattern is associating tags with webpages. You may want to add index pages like `"/tags/clojure"` so you can link to all your webpages about Clojure. Let's call these *tag index webpages*. Nuzzle adds both subdirectory and tag index webpages automatically for all subdirectories and tags present in your site data.

> You may not want to export all the index webpages that Nuzzle adds to your site data. That's ok! You can control which webpages get exported in your webpage rendering function.

What makes these index webpage maps special is that they have an `:index` key with a value that is a vector of webpage map `id`s. For example, the subdirectory index webpage map for the above example would have an `:id` `[:blog-posts]` and an `:index` `[[:blog-posts :foo] [:blog-posts :bar]]`. Inside of your webpage rendering function, you will be able to retrieve data for a webpage by passing its `id` to the function `id->info`. This way, you can retrieve the title and URIs of the indexed pages.

It's worth noting that you can define index webpage maps like any other webpage in your site data. You can use this to add some markup to your index pages from a markdown file by including a `:content` key:

```
[
;; somewhere in your site data EDN ...
  {:id [:blog-posts]
   :content "markdown/blog-posts-index-blurb.md"
   :title "My Awesome Blog Posts"}
]
```

## Creating a Webpage Rendering Function
All webpages are transformed into Hiccup by a single function supplied by the user in the top-level config map under the key `:render-webpage`. This function takes a single argument (a webpage map) and returns a vector of Hiccup.

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `codes.stel.nuzzle.hiccup/raw` wrapper function like so: `(raw "<h1>Title</h1>")`.

Here's an example of a webpage rendering function called `render-webpage`:
```clojure
(defn layout [title & body]
  [:html [:head [:title title]]
   (into [:body] body)])

(defn render-webpage [{:keys [id title render-content] :as webpage}]
  (cond
    ;; Decide what the webpage should look like based on the data in the webpage map
    (= [] id) (layout title [:h1 "Home Page"] [:a {:href "/about"} "About"])
    (= [:about] id) (layout title [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
    (contains? :index webpage) nil
    :else (layout title [:h1 title] (render-content)]))
```

The data contained in that webpage map is crucial when deciding what Hiccup to return. For example, the `render-webpage` function above uses the `:id` value to conditionally render the homepage and the about page.

Just because a webpage exists in your realized site data doesn't mean you have to include it in your static site. If you don't want a webpage to be exported, simply return `nil`. The example above avoids exporting any index webpages by returning `nil` when the webpage map contains an `:index` key.

At the bottom of the function we can see the function from `:render-content` being used. It will always be present in every webpage map. It will either return `nil` or some HTML wrapped in the `codes.stel.nuzzle.hiccup/raw` wrapper. We're able to call it safely in the `:else` clause above because of this trait.

### More data with `id->info`
With many static site generators, accessing global data inside markup templates can be painful. Nuzzle strives to solve this difficult problem elegantly with a function called `id->info`. While realizing your site data, Nuzzle attaches a copy of this function to each webpage map under the key `:id->info`. This function accepts any `id` from your realized site data and returns the corresponding map.

In a word, `id->info` allows us to see the whole world while creating our Hiccup. Since it returns maps from our **realized** site data, all information about the site is at your fingertips. Every webpage and metadata map from your site data is always a function call away.

Better yet, the `id->info` function attaches a copy of itself to each map it returns, so you can always count on it being there. This way you can write functions that accept a webpage or metadata map without having to worry about passing `id->info` along with it.

If `id-info` cannot find the given `id`, it will throw an exception. This makes it easy to spot errors in your code quickly.

There are many use cases for the `id->info` function. It's great for creating index webpages, accessing metadata maps, and many other things:

```clojure
(defn unordered-list [& list-items]
  (->> list-items
       (map (fn [item] [:li item]))
       (into [:ul])))

(defn layout [{:keys [title id->info] :as _webpage} & body]
  (let [{:keys [twitter]} (id->info :social)
        {about-uri :uri} (id->info [:about])]
    [:html [:head [:title title]]
     (into [:body
            [:header
             (unordered-list
              [:a {:href about-uri} "About"]
              [:a {:href twitter} "My Tweets"])]]
           body)]))

(defn render-index-webpage [{:keys [title index id->info] :as webpage}]
  (layout webpage
          [:h1 (str "Index page for " title)]
          (->>
           (for [webpage-id index
                 :let [{:keys [uri title]} (id->info webpage-id)]]
             [:a {:href uri} title])
           (apply unordered-list))))

(defn render-homepage [{:keys [id->info] :as webpage}]
  (layout webpage
          [:h1 "Home Page"]
          [:p (str "Hi there, welcome to my website. If you want to read my rants about Clojure, click ")
            [:a {:href (id->info [:tags :clojure])} "here!"]]))

(defn render-webpage [{:keys [id title render-content id->info] :as webpage}]
  (cond
   (= [] id) (render-homepage webpage)
   (= [:about] id) (layout webpage [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
   (= [:tags :clojure] id) (render-index-webpage webpage)
   :else (layout webpage [:h1 title] (render-content))))
```

## Generating an RSS feed
Nuzzle comes with support for generating an RSS feed. (TODO)
