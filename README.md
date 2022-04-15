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

## Example
Want to read some code? Check out [this repo](https://github.com/stelcodes/dev-blog) which uses Nuzzle to build [this website](https://stel.codes).

## API
All of Nuzzle's functionality is conveniently wrapped up with just three functions in the `codes.stel.nuzzle.api` namespace: `inspect`, `start-server`, and `export`. They all accept the same argument: the `global-config` map.
- `inspect`: Returns a more fleshed-out version of the site configuration with Nuzzle's additions.
- `start-server`: Starts a web server (http-kit) that allows you to use a browser to preview the website. Builds each page from scratch upon each request. Gets a huge power boost when used inside an nREPL.
- `export`: Exports the static site to disk.

## Configuration
### The `global-config` map
The top-level map that unlocks all of Nuzzle's functionality:
```clojure
{
  :site-config <path>       ; required
  :render-page <function>   ; required
  :static-dir <path>        ; defaults to nil (no static assset directory)
  :rss-opts <map>           ; defaults to nil (no RSS feed)
  :remove-drafts? <boolean> ; defaults to false
  :target-dir <path>        ; defaults to "dist"
  :dev-port <int>           ; defaults to 5868
}
```
- `:site-config`: A path to an EDN file. Must be a `site-config` map.
- `:remove-drafts?`: A boolean that indicates whether pages marked as a draft should be removed.
- `:render-page`: A function supplied by the user which is responsible for creating Hiccup for every page of the static site.
- `:static-dir`: A path to a resource directory on the classpath that contains static assets that should be copied into the exported site.
- `:target-dir`: A path to the directory that the site should be exported to. This path does not have to be on the classpath. Defaults to `dist`.
- `:dev-port`: a port number for the development server to listen on. Defaults to 5868.


### The `site-config` map
A `site-config` is an EDN map that defines all the webpages in the website. Nuzzle called each key in this map an `id`.

Here's an annotated example:
```clojure
{
  ;; Keys that are vectors define webpages
  [:about] ; <- This represents the URI "/about"
  {:title "About"} ; <- The value is a map of data associated with the webpage

  [:blog-posts :using-clojure]
  {:title "Using Clojure"
   ;; The special :content key points to a markup file to include
   :content "markdown/using-clojure.md"
   ;; The special :tags key tells Nuzzle about webpage tags
   :tags [:clojure]}

  [:blog-posts :learning-rust]
  {:title "How I Got Started Learning Rust"
   :content "markdown/learning-rust.md"
   :tags [:rust]
   ;; The special :draft? key tells Nuzzle which webpages are drafts
   :draft? true
   ;; The special :rss key tells Nuzzle to include the webpage in the RSS XML file
   :rss true}

  [:blog-posts :clojure-on-fedora]
  {:title "How to Install Clojure on Fedora"
   :content "markdown/clojure-on-fedora.md"
   :tags [:linux :clojure]
   ;; Webpage maps are open, you can include any other data you like
   :foobar "baz"}

  ;; Keyword keys do not represent webpages, just extra data about the website
  :social
  {:twitter "https://twitter.com/username"} ; <- This will be easy to retrieve later
}
```

To reiterate, each key in this map is an `id`. An `id` can either be a vector of keywords: `[:blog-posts :using-clojure]` or a keyword: `:social`.

If the `id` is a **vector of keywords** like `[:blog-posts :using-clojure]`, it represents a **webpage**. The `id` `[:blog-posts :using-clojure]` translates to the URI `"/blog-posts/using-clojure"`. When the website is exported, this webpage will be located at `<target-dir>/blog-posts/using-clojure/index.html`. Webpage maps have some special keys which are all optional:

- `:content`: A path to a resource file on the classpath that contains markup. Nuzzle attaches a `:render-content` key to each webpage map. Maps without a `:content` key will have a `:render-content` function that returns `nil`. Maps with a `:content` key will have a `:render-content` function that returns the generated HTML from the markup file. Nuzzle figures out how to convert the resource to HTML based on the filename suffix. Supported `:content` filetypes are HTML: `.html` and Markdown: `.md`, `.markdown` via [clj-markdown](https://github.com/yogthos/markdown-clj).
- `:tags`: A vector of keywords. Nuzzle analyzes all the tags of all pages and adds tag index pages to the `site-config`. For example, based on this `site-config`, Nuzzle will add these `id`s to the `site-config`: `[:tags :clojure]`, `[:tags :rust]`, `[:tags :linux]`. You can see what `id`s Nuzzle adds using the `codes.stel.nuzzle.api/inspect` function.
- `:draft?`: A boolean indicating whether this page is a draft or not. When true and `:remove-drafts?` is also true, this webpage will not be shown.
- `:rss`: A boolean indicating whether the webpage should be included in the optional RSS feed.
- `:index`: A vector of `id`s. This key is included in all of the auto-generated subdirectory and tag index webpage maps Nuzzle creates on your behalf. Only include this key if you want to create index pages manually.

If the `id` is a **keyword**, the key-value pair is just extra information about the site. It has no effect on the website structure. It can easily be retrieved inside the `:render-page` function later on.

## Creating markup with Hiccup
### What is Hiccup?
Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). Instead of using clunky raw HTML strings that are hard to modify like `"<section id="blog"><h1 class="big">Foo</h1></section>"`, we can use Clojure data types: `[:section {:id "blog"} [:h1 {:class "big"} "Foo"]]`. The basic idea is that all HTML tags are represented as vectors beginning with a keyword that defines the tag's name. After the keyword we can optionally include a map that holds the tag's attributes. We can nest elements by putting a vector inside of another vector. `nil` values are ignored. There is also a shorthand for writing `class` and `id` attributes: `[:section#blog [:h1.big "Foo"]]`. For more information about Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

### Creating a basic `:render-page` function
All webpages are transformed into Hiccup by a single function supplied by the user in the `global-config` map under the key `:render-page`. This function takes a single argument (a webpage map) and returns a vector of Hiccup.

All strings in the Hiccup are escaped, so if you want to add a string of raw HTML, use the `codes.stel.nuzzle.hiccup/raw` wrapper function like so: `(raw "<h1>Title</h1>")`.

Nuzzle adds a `:uri` key to each webpage map that contains the relative link to the webpage like `"/about/"`. Nuzzle also adds an `:id` key with the webpages `id` like `[:about]`. You can use the `:id`, `:uri`, or any other values to decide what Hiccup to return.

Here's an example `:render-page` function:
```clojure
(defn layout [title & body]
  [:html [:head [:title title]]
   (into [:body] body)])

(defn render-page [{:keys [id title render-content] :as page}]
  (cond
    (= [] id) (layout title [:h1 "Home Page"] [:a {:href "/about"} "About"])
    (= [:about] id) (layout title [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
    :else (layout title [:h1 title] (render-content)]))
```

Here we see the `:render-content` function being used. This can always be safely called because Nuzzle guarantees that it will either return `nil` or some HTML from a file wrapped in the `codes.stel.nuzzle.hiccup/raw` wrapper.

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
                            [:a {:href (:twitter (id->info :social)} "Twitter"])
    (= [:about] id) (layout title [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
    (contains? page :index) (render-index page)
    :else (layout title [:h1 title] (render-content)]))
```

A neat thing about `id->info` is that every webpage map returned will also have an `id->info` function attached to it, so any function that accepts a webpage map can rely on it being inside that map.

## Generating an RSS feed
Nuzzle comes with support for generating an RSS feed. (TODO)
