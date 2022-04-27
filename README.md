<p align="center">
<img src="./assets/nuzzle2-with-text.svg" width="400">
</p>
<p align="center">
✨ A data-oriented, REPL-driven static site generator for Clojure ✨
</p>
<hr>

> **WARNING**: Nuzzle is in the alpha stage. Expect API breakages.

## What is Nuzzle?
Nuzzle is a static site generator for people who love:
- Data-oriented functional programming ✨
- REPL-driven workflows 🔁
- Writing prose in Markdown 📝
- The awesome combination of Clojure and Hiccup 💞
- Simplicity 🌷

Nuzzle is a Clojure library, but you could also think about it as a micro-framework. It's goal is to define a simple yet powerful process for turning data and functions into a static site. Nuzzle aims to be the only Clojure dependency that your project requires. It also aims to provide a rich REPL experience that can leverage the power of nREPL clients like Cider and Conjure for extremely fast feedback loops while experimenting with your site appearance and content.

**With Nuzzle you can...**
- Manage all website data and structure inside an EDN file
- Plug in a single function to produce Hiccup
- Retrieve all website data while inside that function
- Write your prose in Markdown files
- Statically render CSS syntax highlighting for Markdown code (requires [Chroma](https://github.com/alecthomas/chroma))
- Utilize a built-in, REPL-driven, hot-reloading web server
- Tag webpages
- Create subdirectory and tag index webpages
- Create an RSS feed

## Real World Example
Want to read some code already? Check out [this repo](https://github.com/stelcodes/dev-blog) which uses Nuzzle to build my personal developer blog deployed at [stel.codes](https://stel.codes).

## Nuzzle's API
Nuzzle's whole interface is just three functions in the `nuzzle.api` namespace:
- `export`: Exports the static site to disk.
- `serve`: Starts a web server (http-kit) for a live preview of the website, building each webpage from scratch upon each request.
- `realize`: Helper function for visualizing your site data after Nuzzle's additions.

All three functions have exactly the same interface:
- They require no arguments.
- They accept keyword arguments which you can use to override the values in your configuration file.

## Configuration File
Nuzzle expects to find an EDN map in the file `nuzzle.edn` in your current working directory which contains some or all of these keys:

- `:site-data` - A vector of maps describing the structure and content of your website. Required.
- `:render-webpage` - A fully qualified symbol pointing to your webpage rendering function. Required.
- `:static-dir` - A path to a directory that contains the static assets for the site. Defaults to nil (no static assets).
- `:output-dir` - A path to a directory to export the site into. Defaults to `out`.
- `:highlight-style` - A string specifying a [Chroma style](https://xyproto.github.io/splash/docs/longer/index.html) for Markdown code syntax highlighting. Defaults to `nil` (no highlighting).
- `:rss-channel` - A map with an RSS channel specification. Defaults to nil (no RSS feed).
- `:remove-drafts?` - A boolean that indicates whether webpages marked as a draft should be removed. Defaults to nil (no draft removal).
- `:dev-port` - A port number for the development server to listen on. Defaults to 6899.

If you're from Pallet town, your `nuzzle.edn` config might look like this:
```clojure
{:static-dir "static"
 :render-webpage views/render-webpage
 :highlight-style "monokai"
 :rss-channel
 {:title "Ash Ketchum's Blog"
  :description "I wanna be the very best, like no one ever was."
  :link "https://ashketchum.com"}
 :site-data
 [
  {:id []
   :markdown "markdown/homepage-blurb.md"}
  {:id [:blog-posts :catching-pikachu]
   :title "How I Caught Pikachu"
   :markdown "markdown/how-i-caught-pikachu.md"}
  {:id [:blog-posts :defeating-misty]
   :title "How I Defeated Misty with Pikachu"
   :markdown "markdown/how-i-defeated-misty.md"}
  {:id [:about]
   :markdown "markdown/about-myself.md"}
  {:id :crypto
   :bitcoin "1GVY5eZvtc5bA6EFEGnpqJeHUC5YaV5dsb"
   :eth "0xc0ffee254729296a45a3885639AC7E10F9d54979"}
  ]
 }
```

## Site Data
The `:site-data` value is the most crucial piece of the Nuzzle config. It defines the structure and details of all the static site's webpages as well as any supplemental information. Site data must be a vector of maps with just one required key: `:id`. The value of `:id` will describe what kind of data that map holds.

If the `:id` is a **vector of keywords**, it represents a typical **webpage**. The `:id` `[:blog-posts :catching-pikachu]` translates to the URI `"/blog-posts/catching-pikachu"` and will be rendered to disk as `<output-dir>/blog-posts/catching-pikachu/index.html`. Nuzzle calls these *webpage maps*.

If the `:id` is a singular **keyword**, the map just contains extra information about the site. It has no effect on the website structure. It can easily be retrieved inside your `render-webpage` function later on. Nuzzle calls these *peripheral maps*.

Here's another annotated example of a `:site-data` value:
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
   ;; The special :markdown key associates a markdown file
   :markdown "markdown/using-clojure.md"
   ;; The special :tags key tells Nuzzle about webpage tags
   :tags [:clojure]}

  {:id [:blog-posts :learning-rust]
   :title "How I Got Started Learning Rust"
   :markdown "markdown/learning-rust.md"
   :tags [:rust]
   ;; The special :draft? key tells Nuzzle which webpages are drafts
   :draft? true
   ;; The special :rss key tells Nuzzle to include the webpage in the RSS XML file
   :rss? true}

  {:id [:blog-posts :clojure-on-fedora]
   :title "How to Install Clojure on Fedora"
   :markdown "markdown/clojure-on-fedora.md"
   :tags [:linux :clojure]
   ;; Webpage maps are open, you can include any data you like
   :foobar "baz"}

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Peripheral Maps

  ;; Extra information not particular to any webpage
  {:id :social
   :twitter "https://twitter.com/clojurerulez"} ; <- This will be easy to retrieve later

  {:id :footer-message
   ;; You can also associate markdown with peripheral maps
   :markdown "markdown/footer-message.md"
]
```

### Special Keys in Webpage Maps
- `:markdown`: A path to an associated markdown file.
- `:tags`: A vector of keywords where each keyword is a tag name.
- `:draft?`: A boolean indicating whether this webpage is a draft or not.
- `:rss?`: A boolean indicating whether the webpage should be included in the optional RSS feed.

### Special Keys in Peripheral Maps
- `:markdown`: A path to an associated markdown file.

## How Nuzzle Adds to the Site Data
You can think of Nuzzle's core functionality as a data pipeline. Nuzzle takes your site data, adds some spice, and sends it to your webpage rendering function, one webpage map at a time.

Nuzzle's process can be visualized like so:
```
   ┌───────────┐           ┌───────────┐               ┌──────────────┐
   │           │           │           │               │              │
   │ Site data │ ────┬───► │ Realized  │ ─────┬─────►  │  Hiccup that │
   │   from    │     │     │ site data │      │        │ is converted │
   │ EDN file  │     │     │           │      │        │ to html and  │
   │           │           │           │               │   exported   │
   └───────────┘  Nuzzle   └───────────┘render-webpage │              │
                 additions                 function    └──────────────┘
```

A key part of this process is the first arrow: Nuzzle's additions. Nuzzle doesn't modify any data in you put into your site data, but it does add to it. Nuzzle calls this **realizing** your site data. The realized site data looks just like the original, but with extra webpage maps and extra keys in the existing ones.

### Webpage Map Additions
Nuzzle adds these keys to every webpage map:
- `:uri`: the path of the webpage from the website's root, (ex `"/blog-posts/learning-clojure/"`).
- `:render-markdown`: A function that renders the webpage's markup if `:markdown` key is present, otherwise returns `nil`.
- `:get-site-data`: A function that allows you to reach into your realized site data inside of your webpage rendering function.

### Peripheral Map Additions
Nuzzle adds these keys to every peripheral map:
- `:render-markdown`: A function that renders the webpage's markup if `:markdown` key is present, otherwise returns `nil`.

### Index Page Additions
Often you'll want to create index webpages in static sites which serve as a webpage that links to other webpages which share a common trait. For example, if you have webpages like `"/blog-posts/foo"` and `"/blog-posts/bar"`, you may want a webpage at `"/blog-posts"` that links to `"/blog-posts/foo"` and `"/blog-posts/bar"`. Let's call these *subdirectory index webpages*. Another common pattern is associating tags with webpages. You may want to add index pages like `"/tags/clojure"` so you can link to all your webpages about Clojure. Let's call these *tag index webpages*. Nuzzle adds both subdirectory and tag index webpages automatically for all subdirectories and tags present in your site data.

> You may not want to export all the index webpages that Nuzzle adds to your site data. That's ok! You can control which webpages get exported in your webpage rendering function.

What makes these index webpage maps special is that they have an `:index` key with a value that is a vector of webpage map `id`s. For example, the subdirectory index webpage map for the above example would have an `:id` `[:blog-posts]` and an `:index` `[[:blog-posts :foo] [:blog-posts :bar]]`. Inside of your webpage rendering function, you will be able to retrieve data for a webpage by passing its `id` to the function `get-site-data`. This way, you can retrieve the title and URIs of the indexed pages.

It's worth noting that you can define index webpage maps like any other webpage in your site data. You can use this to add some markup to your index pages from a markdown file by including a `:markdown` key:

```
[
;; somewhere in your site data EDN ...
  {:id [:blog-posts]
   :markdown "markdown/blog-posts-index-blurb.md"
   :title "My Awesome Blog Posts"}
]
```

## Creating a Webpage Rendering Function
All webpage maps are transformed into Hiccup by a single function supplied by the user in the top-level config map under the key `:render-webpage`. This function takes a single argument (a webpage map) and returns a vector of Hiccup.

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `nuzzle.hiccup/raw` wrapper function like so: `(raw "<h1>Title</h1>")`.

Here's an example of a webpage rendering function called `render-webpage`:
```clojure
(defn layout [title & body]
  [:html [:head [:title title]]
   (into [:body] body)])

(defn render-webpage [{:keys [id title render-markdown] :as webpage}]
  (cond
    ;; Decide what the webpage should look like based on the data in the webpage map
    (= [] id) (layout title [:h1 "Home Page"] [:a {:href "/about"} "About"])
    (= [:about] id) (layout title [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
    (contains? :index webpage) nil
    :else (layout title [:h1 title] (render-markdown)]))
```

The data contained in that webpage map is crucial when deciding what Hiccup to return. For example, the `render-webpage` function above uses the `:id` value to conditionally render the homepage and the about page.

Just because a webpage exists in your realized site data doesn't mean you have to include it in your static site. If you don't want a webpage to be exported, simply return `nil`. The example above avoids exporting any index webpages by returning `nil` when the webpage map contains an `:index` key.

At the bottom of the function we can see the function from `:render-markdown` being used. It will always be present in every webpage map. It will either return `nil` or some HTML wrapped in the `nuzzle.hiccup/raw` wrapper. We're able to call it safely in the `:else` clause above because of this trait.

### Accessing Your Site Data with `get-site-data`
With many static site generators, accessing global data inside markup templates can be painful to say the least. Nuzzle strives to solve this difficult problem elegantly with a function called `get-site-data`. While realizing your site data, Nuzzle attaches a copy of this function to each webpage map under the key `:get-site-data`.

In a word, `get-site-data` allows us to see the whole world while creating our Hiccup. It has two forms:

1. `(get-site-data)`: With no arguments, returns the whole realized site data vector.
2. `(get-site-data [:blog-posts])`: With an `id` from the realized site data, return the corresponding map.

Since `get-site-data` returns maps from our *realized* site data, all information about the site is at your fingertips. Every webpage and peripheral map from your site data is always a function call away.

To make things even more convenient, the `get-site-data` function attaches a copy of itself to each map it returns. This makes it easy to write functions that accept a webpage or peripheral map without ever having to worry about passing `get-site-data` along with it. It's kind of like a self-replicating bridge between all your site data maps.

> If `get-site-data` cannot find the given `id`, it will throw an exception. This makes it easy to spot errors in your code quickly.

There are many use cases for the `get-site-data` function. It's great for creating index webpages, accessing peripheral maps, and countless other things:

```clojure
(defn unordered-list [& list-items]
  (->> list-items
       (map (fn [item] [:li item]))
       (into [:ul])))

(defn layout [{:keys [title get-site-data] :as _webpage} & body]
  (let [{:keys [twitter]} (get-site-data :social)
        {about-uri :uri} (get-site-data [:about])]
    [:html [:head [:title title]]
     (into [:body
            [:header
             (unordered-list
              [:a {:href about-uri} "About"]
              [:a {:href twitter} "My Tweets"])]]
           body)]))

(defn render-index-webpage [{:keys [title index get-site-data] :as webpage}]
  (layout webpage
          [:h1 (str "Index page for " title)]
          (->>
           (for [id index
                 :let [{:keys [uri title]} (get-site-data id)]]
             [:a {:href uri} title])
           (apply unordered-list))))

(defn render-homepage [{:keys [get-site-data] :as webpage}]
  (layout webpage
          [:h1 "Home Page"]
          [:p (str "Hi there, welcome to my website. If you want to read my rants about Clojure, click ")
            [:a {:href (-> [:tags :clojure] get-site-data :uri)} "here!"]]))

(defn render-webpage [{:keys [id title render-markdown] :as webpage}]
  (cond
   (= [] id) (render-homepage webpage)
   (= [:about] id) (layout webpage [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
   (= [:tags :clojure] id) (render-index-webpage webpage)
   :else (layout webpage [:h1 title] (render-markdown))))
```

## Generating an RSS feed
Nuzzle comes with support for generating an RSS feed. (TODO)
