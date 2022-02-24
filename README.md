# Nuzzle ✨🐈
A data-oriented micro-framework for quickly creating static sites with Clojure and Hiccup.

## Design Goals
Nuzzle aims to allow the user to...
- describe a static web site with EDN
- use Hiccup to make static web pages with minimal boilerplate
- easily retrieve all website information while creating Hiccup
- set up a REPL-based hot-reloading workflow quickly and easily
- painlessly create subdirectory index pages
- painlessly tag web pages and create tag index pages
- enjoy extremely simple configuration requirements

## API
All of Nuzzle's API is just three functions in the `codes.stel.nuzzle.api` namespace: `start-server`, `inspect` and `export`. All three of these functions accept one argument: the `global-config` map.
- `start-server`: A helper function for development. Starts a `http-kit` server which builds each page from scratch upon each request. Returns a function to stop the server.
- `inspect`: A helper function for development. Returns a modified `site-config` map with all the modifications Nuzzle makes before sending the page data to the user's rendering function. These additions include removing drafts, adding tag index pages, adding group index pages, and adding `:uri` and `:render-content-fn` keys where appropriate.
- `export`: Exports the static site to disk, creating `:target-dir` if necessary. Copies the contents of `:static-dir` into `:target-dir`.

## Configuration
### `global-config`
Nuzzle has a simple global configuration map that unlocks all of Nuzzle's functionality:
```clojure
{
  :site-config <path>
  :remove-drafts? <boolean>
  :render-fn <function>
  :static-dir <path>
  :target-dir <path>
  :dev-port <int>
}
```
- `:site-config`: a path to an `.edn` resource on the classpath. Must be a map conforming to the `site-config` spec. This map defines the structure and content of the static site.
- `:remove-drafts?`: a boolean that indicates whether pages in the `site-config` map with `:draft? true` should be removed.
- `:render-fn`: a function supplied by the user which is responsible for creating Hiccup for every page of the static site.
- `:static-dir`: a path to a resource directory on the classpath that contains static assets that should be copied into the exported site.
- `:target-dir`: a path to the directory that the site should be exported to. This path does not have to be on the classpath. Defaults to `dist`.
- `:dev-port`: a port number for the development server to listen on. Defaults to 5868.


### `site-config`
A `site-config` is an EDN map that is kept in an file in your `resources` directory or somwhere else on your classpath.

Here's an example:
```clojure
{
  [:about]
  {:title "About"}

  [:blog-posts :using-clojure]
  {:title "Using Clojure"
   :resource "markdown/using-clojure.md"
   :tags [:clojure]}

  [:blog-posts :learning-rust]
  {:title "How I Got Started Learning Rust"
   :resource "markdown/learning-rust.md"
   :tags [:rust]
   :draft? true}

  [:blog-posts :clojure-on-fedora]
  {:title "How to Install Clojure on Fedora"
   :resource "markdown/clojure-on-fedora.md"
   :tags [:linux :clojure]}

  :social
  {:twitter "https://twitter.com/username"}
}
```

Each key in this map is called an `id`. Each `id` can either be a keyword (`:social`) or a vector of keywords (`[:blog-posts :using-clojure]`).

If the `id` is a keyword, the key-value pair is just extra information about the site. It has no effect on the website structure. It can easily be retrieved while you're creating Hiccup inside the `:render-fn` function from our `global-config`.

If the `id` is a vector like `[:blog-posts :using-clojure]`, it represents a page of the web site. The key `[:blog-posts :using-clojure]` translates to the URI `"/blog-posts/using-clojure"`. Cool right? From now on we'll call these **pages**. Pages have some special keys which are all optional:

- `:content`: a path to a resource file on the classpath that contains the page's contents. Pages with a `:content` key will have another key added called `:render-content-fn` whose value is a function that returns a string of raw html. Nuzzle figures out how to convert the resource to HTML based on the filetype extension. Supported `:content` filetypes are HTML: (`.html`) and Markdown (`.md`, `.markdown`) via [clj-markdown](https://github.com/yogthos/markdown-clj).
- `:tags`: a vector of keywords. The keywords can be anything you like. Nuzzle analyzes all the tags of all pages and adds tag index pages to the `site-config`. For example, based on this `site-config`, Nuzzle will add these pages: `[:tags :clojure]`, `[:tags :rust]`, `[:tags :linux]`.
- `:draft?`: a boolean indicating whether this page is a draft or not. Pages with `:draft? true` are removed when the `global-config` contains `:remove-drafts? true`.

## Turning Pages into Hiccup
### What is Hiccup?
Hiccup is a method for representing HTML using Clojure datastructures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). Instead of using clunky raw HTML strings that are hard to modify like `"<section id="blog"><h1 class="big">Foo</h1></section>"`, we can simply use Clojure datastructures: `[:section {:id "blog"} [:h1 {:class "big"} "Foo"]]`. The basic idea is that all HTML tags are represented as vectors beginning with a keyword that defines the tag's name. After the keyword we can optionally include a map that holds the tag's attributes. We can nest elements by putting a vector inside of another vector. There is also a shorthand for writing `class` and `id` attributes: `[:section#blog [:h1.big "Foo"]]`. As you can see, Hiccup is terse yet highly flexible. For more information about Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

### Creating a `:render-fn`
In Nuzzle, all pages are transformed into Hiccup by a single function supplied by the user. This is the job of the `:render-fn` from the `global-config`. The user creates a single function that is capable of producing any page into Hiccup. This function takes a single argument (a map) and returns a vector of Hiccup.

> **Note:** Nuzzle puts the `id` of every page under the key `:id` before passing the page to the `:render-fn`.

Here's an extremely simple rendering function:
```clojure
(defn render [{:keys [id title render-content-fn] :as page}]
  (cond
    (= [] id) [:html [:h1 "Home Page"]]
    (= [:about] id) [:html [:h1 "About Page"]]
    :else [:html [:h1 title] (render-content-fn)]))
```
