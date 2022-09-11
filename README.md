<p align="center">
<img src="./assets/nuzzle2-with-text.svg" width="400">
</p>
<p align="center">
✨ A data-oriented, REPL-driven static site generator for Clojure ✨
</p>
<div align="center">
  <img src="https://github.com/stelcodes/nuzzle/actions/workflows/unit-tests.yaml/badge.svg" alt="unit tests">
  <img src="https://github.com/stelcodes/nuzzle/actions/workflows/clj-kondo.yaml/badge.svg" alt="clj-kondo">
  <a href="https://clojars.org/codes.stel/nuzzle"><img src="https://img.shields.io/clojars/v/codes.stel/nuzzle.svg" alt="Clojars Project"></a>
</div>
<hr>

> **WARNING**: Nuzzle is in the alpha stage. Expect API breakages.

## What is Nuzzle?
Nuzzle is a static site generator for people who love:
- Data-oriented functional programming ✨
- REPL-driven workflows 🔁
- The awesome combination of Clojure and Hiccup 💞
- Simplicity 🌷

Nuzzle is a Clojure library, but you could also think about it as a micro-framework. It's goal is to define a simple yet powerful process for turning data and functions into a static site.

**With Nuzzle you can...**
- Manage all website data and structure declaratively in an EDN file
- Plug in a single function to produce HTML markup (via Hiccup) for every page of the website
- Easily retrieve all website data while inside that function
- Utilize a built-in, REPL-driven, hot-reloading web server for lightning-fast development feedback
- Statically render syntax highlighting for Markdown code blocks (requires either [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma))
- Generate index pages for page groupings and tags
- Generate an Atom feed
- Generate a sitemap

## Real World Example
Want to read some code already? Check out [this repo](https://github.com/stelcodes/dev-blog) which uses Nuzzle to build my personal developer blog deployed at [stel.codes](https://stel.codes).

## Requirements
- Java >= 11
- Clojure >= 1.11.1
- [Pygments](https://github.com/pygments/pygments) >= 2.12.0 (optional)
- [Chroma](https://github.com/alecthomas/chroma) >= 2.0.0 (optional)

## Usage
```
clj -Sdeps '{:deps {codes.stel/nuzzle {:mvn/version "0.5.320"}}}'
```

```clojure
(require '[nuzzle.api :as nuzz])

;; Create a config map
(def config {...})

;; Start development server
;; Pass the config as a var to get awesome hot-reloading capabilities!
;; The returned value is a function that stops the server.
(nuzz/serve #'config)

;; Publish the static site, returns nil
(nuzz/publish config)

;; Prints a diff of all changes Nuzzle made to the config before using it, returns nil
(nuzz/transform-diff config)
```

Nuzzle's whole interface is just four functions in the `nuzzle.api` namespace:
- `(nuzzle.api.publish config {:keys [base-url overlay-dir publish-dir]})`: Exports the static site to disk.
  - `base-url` - Optional but required for sitemap and Atom feed generation. URL where site will be hosted. Must start with "http://" or "https://".
  - `overlay-dir` - Optional path to a directory that will be overlayed on top of the static web site, useful for including static assets. Defaults to `nil` (no overlay).
  - `publish-dir` - Optional path to a directory to publish the site into. Be careful, all prior contents of this directory will be lost! Defaults to `"dist"`.
- `(nuzzle.api.serve config {:keys [overlay-dir port]})`: Starts a web server (http-kit) for a live preview of the website, building each page from scratch upon each request.
  - `overlay-dir` - Same as above.
  - `port`: Optional port number for server to listen on. Defaults to `6899`.
- `(nuzzle.api.transform config)`: Returns your config after Nuzzle's transformations.
- `(nuzzle.api.transform-diff config)`: Pretty prints a colorized diff of your config before and after Nuzzle's transformations.

## Configuration Map
Nuzzle builds your static site from a map of configuration values. The config is validated by `clojure.spec`. You can find the [config spec here](https://github.com/stelcodes/nuzzle/blob/main/src/nuzzle/schemas.clj).

If you're from Pallet town, your config might look like this:
```clojure
{:nuzzle/render-page views/render-page

 []
 {:nuzzle/title "Home"
  :nuzzle/content "markdown/homepage-introduction.md"}

 [:blog-posts]
 {:nuzzle/title "Blog Posts"
  :nuzzle/content "markdown/blog-header.md"}

 [:blog-posts :catching-pikachu]
 {:nuzzle/title "How I Caught Pikachu"
  :nuzzle/content "markdown/how-i-caught-pikachu.md"}

 [:blog-posts :defeating-misty]
 {:nuzzle/title "How I Defeated Misty with Pikachu"
  :nuzzle/content "markdown/how-i-defeated-misty.md"
  :nuzzle/draft? true}

 [:about]
 {:nuzzle/title "About Ash"
  :nuzzle/content "markdown/about-ash.md"}}
```

## Option and Page Entries

> **Note:** An "entry" refers to a key-value pair of a map

The Nuzzle config must be a map where each key is either a keyword or a vector of keywords. The distinction is important. It separates map entries into two categories:

1. **Option Entries:** Option entries have a **keyword** key and are usually defined by Nuzzle, but you can also include your own custom option entries as well. The associated value can be of any type. Here is a brief summary of all the option entry keys specified by Nuzzle:

- `:nuzzle/render-page` - A fully qualified symbol that must resolve to your page rendering function. Required.
- `:nuzzle/build-drafts?` - A boolean that indicates whether pages marked as a draft should be included. Defaults to `false` (no drafts included).

The following config options provide functionality above and beyond basic static site generation. They are totally optional.

- `:nuzzle/atom-feed` - Enables Atom feed generation. Value is a map of feed metadata. Defaults to `nil` (no Atom feed).
- `:nuzzle/syntax-highlighter` - Enables syntax highlighting for language-tagged Markdown code blocks. Value is a map of highlighting options. Defaults to `nil` (no syntax highlighting).
- `:nuzzle/custom-elements` - Enables custom hiccup processing. Value is a map of keyword element tags to symbols which must resolve to functions that accept and return a Hiccup vector. Defaults to `{}` (no custom elements).

2. **Page Entries:** Page entries have a key that is a **vector of keywords**, and their associated value must be a map. Each entry represents a single page of the website. The key of the entry represents the URL of the web page: `[:blog-posts :catching-pikachu]` translates to the URL `"/blog-posts/catching-pikachu"` and will be rendered to disk as `<publish-dir>/blog-posts/catching-pikachu/index.html`. Here is a summary of all the page entry keys specified by Nuzzle to be used inside the associated map value:

- `:nuzzle/title`: A title for the web page. Required.
- `:nuzzle/content`: A path to an associated Markdown or HTML file.
- `:nuzzle/draft?`: A boolean indicating whether this page is a draft or not.
- `:nuzzle/feed?`: A boolean indicating whether the page should be included in the optional Atom feed.
- `:nuzzle/tags`: A set of keywords where each keyword represents a tag name.
- `:nuzzle/updated`: A timestamp string representing when the page was last updated.
- `:nuzzle/summary`: A string summary of the page content.
- `:nuzzle/author`: A map representing the author of the page. Must have a `:name`, optionally `:email` and `:url`.

## Understanding the Nuzzle Config

Here's another example config with annotations:
```clojure
{;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ;; Option Entries

 :nuzzle/render-page views/render-page

 ;; Custom option entries can be anything you like
 :twitter "https://twitter.com/clojurerulez" ; <- This will be easy to retrieve later

 ;; You can also associate content with custom option entries!
 :footer-message
 {:nuzzle/content "markdown/footer-message.md"}

 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ;; Page Entries

 ;; The homepage
 [] ; <- This represents the URL "/"
 {:nuzzle/title "Home"}

 [:about] ; <- This represents the URL "/about"
 {:nuzzle/title "About"} ; <- Titles are necessary for page entries

 [:blog-posts :using-clojure]
 {:nuzzle/title "Using Clojure"
  ;; The optional :nuzzle/content key associates a Markdown or HTML file
  :nuzzle/content "markdown/using-clojure.md"
  ;; The optional :nuzzle/tags key tells Nuzzle about page tags
  :nuzzle/tags #{:clojure}}

 [:blog-posts :learning-rust]
 {:nuzzle/title "How I Got Started Learning Rust"
  :nuzzle/content "markdown/learning-rust.md"
  :nuzzle/tags #{:rust}
  ;; The optional :nuzzle/draft? key tells Nuzzle which pages are drafts
  :nuzzle/draft? true
  ;; The optional :feed? key tells Nuzzle to include the page in the Atom feed XML file
  :nuzzle/feed? true}

 [:blog-posts :clojure-on-fedora]
 {:nuzzle/title "How to Install Clojure on Fedora"
  :nuzzle/content "markdown/clojure-on-fedora.md"
  :nuzzle/tags #{:linux :clojure}
  ;; Page maps are open, you can include any data you like
  :foobar "baz"}}
```

## How Nuzzle Transforms Your Config
You can think of Nuzzle's core functionality as a data pipeline. Nuzzle starts with the config map in your `nuzzle.edn`, adds some spice, sends it through your page rendering function, and exports the results to disk.

Nuzzle's data pipeline can be visualized like so:
```
   ┌───────────┐           ┌───────────┐               ┌──────────────┐
   │           │           │           │               │              │
   │  Config   │ ────┬───► │Transformed│ ─────┬─────►  │  Hiccup that │
   │   from    │     │     │ config    │      │        │ is converted │
   │ nuzzle.edn│     │     │           │      │        │ to HTML and  │
   │           │     │     │           │      │        │  published   │
   └───────────┘           └───────────┘               └──────────────┘
                  Nuzzle              :nuzzle/render-page
              transformations             function
```

A key part of this process is the first arrow: Nuzzle's transformations. Nuzzle also calls this step **transforming** your config. A transformed config looks very similar to the original, but with extra page entries and extra keys in the existing page entries.

### Adding Keys to Page Entries
Nuzzle adds these keys to every page map:
- `:nuzzle/url`: The key of the page (the vector of keywords representing the URL) is added to the page map.
- `:nuzzle/index`: A set of page entry keys that should be indexed by the respective web page.
- `:nuzzle/render-content`: A function that returns the page's associated content file converted to HTML (if `:nuzzle/content` key is present, otherwise returns `nil`).
- `:nuzzle/get-config`: A function that allows you to freely access your Nuzzle config from inside your page rendering function.

### Adding Keys to Option Entries
- `:nuzzle/render-content`: Same as above but only added if the associated value is a map and `:nuzzle/content` is present. Can be used to create content snippets to use on several pages.

### Adding More Page Entries (Index Pages)
Often people want to create index pages in static sites which link to other pages that share a common trait. Nuzzle calls these **index pages**. Nuzzle has two categories for index pages:

1. **Hierarchical Index Pages:** For every subdirectory in your website, their will be a natural hierarchy of content. For example, `"/blog-posts/foo"` and `"/blog-posts/bar"` naturally have a shared hierarchical index page at `"/blog-posts"`.

2. **Tag Index Pages:** Another common pattern is associating tags with pages. A page like `"/tags/clojure"` indexes all your web pages about Clojure.

Nuzzle adds both hierarchical and tag index pages automatically for all hierarchies and tags present in your website. This example illustrates how Nuzzle adds both types of index page entries to a Nuzzle config:

```clojure
;; Example config (Some page keys omitted for brevity)

{[:recipes :grilled-cheese]
 {:nuzzle/title "My World Famous Grilled Cheese"
  :nuzzle/tags #{:comfort-food}}

 [:recipes :ramen-noodles]
 {:nuzzle/title "The Best Ramen Noodle Recipe"
  :nuzzle/tags #{:comfort-food :japanese}}}

;; After transformations:

{[:recipes :grilled-cheese]
 {:nuzzle/title "My World Famous Grilled Cheese"
  :nuzzle/tags #{:comfort-food}}

 [:recipes :ramen-noodles]
 {:nuzzle/title "The Best Ramen Noodle Recipe"
  :nuzzle/tags #{:comfort-food :japanese}}}

 [:recipes]
 {:nuzzle/title "Recipes"
  :nuzzle/index #{[:recipes :grilled-cheese] [:recipes :ramen-noodles]}}

 [:tags :comfort-food]
 {:nuzzle/title "#comfort-food"
  :nuzzle/index #{[:recipes :grilled-cheese] [:recipes :ramen-noodles]}}

 [:tags :japanese]
 {:nuzzle/title "#japanese"
  :nuzzle/index #{[:recipes :ramen-noodles]}}

 [:tags]
 {:nuzzle/title "Tags"
  :nuzzle/index #{[:tags :comfort-food] [:tags :japanese]}}}
```

These added index page entries have an `:nuzzle/index` key with a value that is a set of page entry keys.

It's worth noting that you can include index page entries in your Nuzzle config yourself, just like any other page. In this case Nuzzle will just add the `:nuzzle/index` entry to the page value you've already defined. This is useful for specifying a non-standard title or adding content to your index pages:

```clojure
;; Nuzzle will append an :nuzzle/index key later if there are any pages under the recipes subdirectory
[:recipes]
{:nuzzle/content "markdown/recipes-introduction.md"
 :nuzzle/title "All My Yummy Recipes!"}
```

## Creating a Page Rendering Function
All page entries are transformed into Hiccup by a single function. This function takes a single argument (a page entry map) and returns a vector of Hiccup.

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

Here's an example of a page rendering function called `simple-render-page`:
```clojure
(defn layout [title & body]
  [:html [:head [:title title]]
   (into [:body] body)])

(defn simple-render-page
  [{:nuzzle/keys [url render-content title] :as _page}]
  (case url
    ;; Decide what the page should look like based on the url
    []       (layout title [:h1 title] [:a {:href "/about"} "About"])
    [:about] (layout title [:h1 title] [:p "nuzzle nuzzle uwu :3"])
    ;; Default:
    (layout title [:h1 title] (render-content)))
```

The `render-page` function uses the `:nuzzle/url` value to determine what Hiccup to return. This is how a single function can produce Hiccup for every page.

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `nuzzle.hiccup/raw` wrapper function like so: `(raw "<h1>Title</h1>")`.

### Accessing the Config with `get-config`
With many static site generators, accessing global data inside markup templates can be *painful*. Since Nuzzle is heavily data-oriented, this problem becomes much easier to solve.

Instead of requiring your page rendering function to accept multiple arguments (ex: one for the page entry map, one for the whole config), Nuzzle adds a function to each page entry map passed to your page rendering function under the key `:nuzzle/get-config`.

In a word, `get-config` allows us to see the whole world while creating our Hiccup and let's us know if we are looking for something that doesn't exist. It has two forms:

1. With no arguments, returns the whole transformed config map.
```clojure
(get-config)
```
2. With one or more arguments, uses arguments one by one as keys to dive into the config map. This is similar to `clojure.core/get-in` but will throw an exception if a key does not exist. You can use this behavior of `get-config` to guarantee a value exists.
```clojure
(get-config [:blog-posts])
(get-config [:blog-posts] :nuzzle/url)
(get-config :my-custom-option)
```

Since `get-config` returns values from our *transformed* config, all information about the site is at your fingertips. Every option and page entry from your config is always a single function call away.

Of course, any page entry map returned from `get-config` will also have a `:nuzzle/get-config` key attached to it. This naturally lends itself to a convention where most Hiccup-generating functions can accept a page entry map as its first or only argument while still being able to access any data in your Nuzzle config.

There are many use cases for the `get-config` function. It's great for creating index pages, accessing custom option entries, and countless other things:

```clojure
(ns foobar.markup
  (:require [nuzzle.util :refer [stringify-url]]))

(def twitter-url "https://twitter.com/foobar")

(defn unordered-list [& list-items]
  (->> list-items
       (map (fn [item] [:li item]))
       (into [:ul])))

(defn layout [{:nuzzle/keys [title] :as _page} & body]
  [:html [:head [:title title]]
   (into [:body
          [:header
           (unordered-list
            [:a {:href (stringify-url [:about])} "About"]
            [:a {:href twitter-url} "My Tweets"])]]
         body)])

(defn render-index-page [{:nuzzle/keys [title index get-config] :as page}]
  (layout page
          [:h1 (str "Index page for " title)]
          (->>
           (for [url index]
             [:a {:href (stringify-url url)} (get-config url :nuzzle/title)])
           (apply unordered-list))))

(defn render-homepage [page]
  (layout page
          [:h1 "Home Page"]
          [:p (str "Hi there, welcome to my website. If you want to read my rants about Clojure, click ")
           [:a {:href (stringify-url [:tags :clojure])} "here!"]]))

(defn render-page [{:nuzzle/keys [url render-content title index] :as page}]
  (cond
   (= [] url)       (render-homepage page)
   (= [:about] url) (layout page [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
   index                 (render-index-page page)
   :default              (layout page [:h1 title] (render-content))))
```

## Syntax Highlighting
Syntax-highlighted code can give your website a polished, sophisticated appearance. Nuzzle let's you painlessly plug your Markdown code-blocks into [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma). Nuzzle uses `clojure.java.shell` to interact with these programs. Since they are not available as Java or Clojure libraries, Nuzzle users must manually install them into their $PATH in order for Nuzzle to use them.

Syntax highlighting is controlled via the config setting `:nuzzle/syntax-highlighter` which is expected to be a map with these keys:
- `:provider` - A keyword specifying the program to use (either `:chroma` or `:pygments`). Required.
- `:style` - A string specifying a style for Markdown code syntax highlighting. Defaults to `nil` (HTML classes only).
- `:line-numbers?` - A boolean indicating whether line numbers should be included.

When this map is present, Nuzzle will run you code-blocks with language annotations through the chosen syntax highlighting program. If the program supports the language found in the language annotation (check their lexers list), it will output the code as HTML with the different syntax tokens wrapped in `span` elements.

If `:style` is `nil`, the programs will just attach classes to these `span` elements and it's up to you to define their CSS. If `:style` is specified, the programs will attempt to apply that style as inline CSS.

Background colors are not included. Nuzzle adds the class `code-block` to every `code` element that is a code-block so you can define your own code-block background color using CSS like:
```css
code.code-block { background-color: darkslategray; }
```

#### Pygments
[Pygments](https://github.com/pygments/pygments) is a popular and awesome syntax highlighting program written in Python. You can install Pygments with `pip`:
```shell
pip install --user pygments
```
This should install Pygment's CLI app called `pygmentize` into your $PATH. There are also [many packages available](https://pypi.org/search/?q=pygments&o=) that define additional styles and lexers:
```shell
pip install --user pygments-base16 pygments-httpie
```
These become available to `pygmentize` simply by installing them. You can run `pygmentize -L` to see all styles and lexers available or check out the [default Pygments style gallery](https://pygments.org/styles/).

Nuzzle supports Pygments versions 2.12.0 and above.

#### Chroma
[Chroma](https://github.com/alecthomas/chroma) is a syntax highlighter written in Go and based heavily on Pygments. Download with your favorite package manager, or alternatively the author [suggests](https://github.com/alecthomas/chroma/issues/533) downloading the `chroma` binary via their [GitHub releases](https://github.com/alecthomas/chroma/releases).

Besides a browser, you could also use `wget` or `curl`:
```shell
wget https://github.com/alecthomas/chroma/releases/download/v2.0.0-alpha4/chroma-2.0.0-alpha4-linux-amd64.tar.gz

curl --location https://github.com/alecthomas/chroma/releases/download/v2.0.0-alpha4/chroma-2.0.0-alpha4-linux-amd64.tar.gz --output chroma-2.0.0-alpha4-linux-amd64.tar.gz
```

You can run `chroma --list` to see all styles and lexers available or check out the [Chroma style gallery](https://xyproto.github.io/splash/docs/longer/index.html).

Nuzzle supports Chroma versions 2.0.0 and above.
