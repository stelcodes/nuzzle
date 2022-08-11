<p align="center">
<img src="./assets/nuzzle2-with-text.svg" width="400">
</p>
<p align="center">
✨ A data-oriented, REPL-driven static site generator for Clojure ✨
</p>
<div align="center">
  <img src="https://github.com/stelcodes/nuzzle/actions/workflows/unit-tests.yaml/badge.svg" alt="unit tests">
  <img src="https://github.com/stelcodes/nuzzle/actions/workflows/clj-kondo.yaml/badge.svg" alt="clj-kondo">
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
- Manage all website data and structure inside an EDN file
- Plug in a single function to produce Hiccup
- Retrieve all website data while inside that function
- Statically render CSS syntax highlighting for Markdown code-blocks (requires either [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma))
- Utilize a built-in, REPL-driven, hot-reloading web server
- Tag pages
- Create subdirectory and tag index pages
- Create an RSS feed

## Real World Example
Want to read some code already? Check out [this repo](https://github.com/stelcodes/dev-blog) which uses Nuzzle to build my personal developer blog deployed at [stel.codes](https://stel.codes).

## Requirements
- Java >= 11
- Clojure >= 1.11.1
- [Pygments](https://github.com/pygments/pygments) >= 2.12.0 (optional)
- [Chroma](https://github.com/alecthomas/chroma) >= 2.0.0 (optional)

## Nuzzle's API
Nuzzle's whole interface is just three functions in the `nuzzle.api` namespace:
- `publish`: Exports the static site to disk.
- `serve`: Starts a web server (http-kit) for a live preview of the website, building each page from scratch upon each request.
- `realize`: Helper function for visualizing your site data after Nuzzle's additions.

All three functions have exactly the same interface:
- They require no arguments.
- They accept keyword arguments which you can use to override the values in your configuration file.

## Configuration File
Nuzzle expects to find an EDN map in the file `nuzzle.edn` in your current working directory containing these keys:

- `:nuzzle/base-url` - URL where site will be hosted. Must start with "http://" or "https://". Required.
- `:nuzzle/render-page` - A fully qualified symbol pointing to your page rendering function. Required.
- `:nuzzle/publish-dir` - A path to a directory to publish the site into. Defaults to `"out"`.
- `:nuzzle/overlay-dir` - A path to a directory that will be overlayed on top of the `:nuzzle/publish-dir` directory as the final stage of publishing. Defaults to `nil` (no overlay).
- `:nuzzle/syntax-highlighter` - A map of syntax highlighting options for language-tagged code blocks. Defaults to `nil` (no syntax highlighting).
- `:nuzzle/rss-channel` - A map with an RSS channel specification. Defaults to `nil` (no RSS feed).
- `:nuzzle/build-drafts?` - A boolean that indicates whether pages marked as a draft should be included. Defaults to `nil` (no drafts included).
- `:nuzzle/custom-elements` - A map of keywords -> symbols which define functions to transform the Hiccup representation of custom HTML elements.
- `:nuzzle/server-port` - A port number for the development server to listen on. Defaults to 6899.

If you're from Pallet town, your `nuzzle.edn` config might look like this:
```clojure
{:nuzzle/base-url "https://ashketchum.com"
 :nuzzle/render-page views/render-page

 []
 {:nuzzle/title "Home"
  :nuzzle/content "markdown/homepage-introduction.md"}

 [:blog-posts :catching-pikachu]
 {:nuzzle/title "How I Caught Pikachu"
  :nuzzle/content "markdown/how-i-caught-pikachu.md"}

 [:blog-posts :defeating-misty]
 {:nuzzle/title "How I Defeated Misty with Pikachu"
  :nuzzle/content "markdown/how-i-defeated-misty.md"}

 [:about]
 {:nuzzle/title "About Ash"
  :nuzzle/content "markdown/about-ash.md"}}
```

## Option and Page Entries

> **Note:** An "entry" refers to a key-value pair of a map

The Nuzzle config data-structure must be a map where each key is either a keyword or a vector of keywords. The distinction is important. It separates map entries into two categories:

1. Option Entries
Option entries have a **keyword** key and are usually defined by Nuzzle (ex: `:nuzzle/base-url`), but you can also include your own option entries as well. The associated value can be of any type.

2. Page Entries
Page entries have a key that is a **vector of keywords**, and their associated value must be a map that represents a single page of the website. The key represents the URL of the web page: `[:blog-posts :catching-pikachu]` translates to the URL `"/blog-posts/catching-pikachu"` and will be rendered to disk as `<publish-dir>/blog-posts/catching-pikachu/index.html`.

## Special Page Entry Keys

Here's another example config with annotations:
```clojure
{;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ;; Option Entries

 :nuzzle/base-url "https://example.com"
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
  ;; The optional :rss key tells Nuzzle to include the page in the RSS XML file
  :nuzzle/rss? true}

 [:blog-posts :clojure-on-fedora]
 {:nuzzle/title "How to Install Clojure on Fedora"
  :nuzzle/content "markdown/clojure-on-fedora.md"
  :nuzzle/tags #{:linux :clojure}
  ;; Page maps are open, you can include any data you like
  :foobar "baz"}}
```

### Special Keys in Page Entry Maps
- `:nuzzle/content`: A path to an associated Markdown or HTML file.
- `:nuzzle/tags`: A set of keywords where each keyword is a tag name.
- `:nuzzle/draft?`: A boolean indicating whether this page is a draft or not.
- `:nuzzle/rss?`: A boolean indicating whether the page should be included in the optional RSS feed.

### Special Keys in Custom Option Entry Maps
- `:nuzzle/content`: A path to an associated Markdown or HTML file.

## How Nuzzle Transforms Your Config
You can think of Nuzzle's core functionality as a data pipeline. Nuzzle starts with the config map in your `nuzzle.edn`, adds some spice, sends it through your page rendering function, and exports the results to disk.

Nuzzle's data pipeline can be visualized like so:
```
   ┌───────────┐           ┌───────────┐               ┌──────────────┐
   │           │           │           │               │              │
   │  Config   │ ────┬───► │ Realized  │ ─────┬─────►  │  Hiccup that │
   │   from    │     │     │ config    │      │        │ is converted │
   │ nuzzle.edn│     │     │           │      │        │ to HTML and  │
   │           │     │     │           │      │        │  published   │
   └───────────┘           └───────────┘               └──────────────┘
                  Nuzzle              :nuzzle/render-page
              transformations             function
```

A key part of this process is the first arrow: Nuzzle's transformations. Nuzzle also calls this step **realizing** your config. A realized config looks very similar to the original, but with extra page entries and extra keys in the existing page entries.

### Adding Keys to Page Entries
Nuzzle adds these keys to every page map:
- `:nuzzle/url`: the path of the page from the website's root without the `index.html` part (ex `"/blog-posts/learning-clojure/"`).
- `:nuzzle/render-content`: A function that renders the page's associated content file if `:nuzzle/content` key is present, otherwise returns `nil`.
- `:get-config`: A function that allows you to freely reach into your site data from inside of your page rendering function.

### Adding Keys to Option Entries
Nuzzle adds these keys to every peripheral map:
- `:nuzzle/render-content`: Same as above.
- `:get-config`: Same as above.

### Adding Page Entries (Index Pages)
Often people want to create index pages in static sites which serve as a page that links to other pages which share a common trait. For example, if you have pages like `"/blog-posts/foo"` and `"/blog-posts/bar"`, you may want a page at `"/blog-posts"` that links to `"/blog-posts/foo"` and `"/blog-posts/bar"`. Nuzzle calls these **subdirectory index pages**.

Another common pattern is associating tags with pages. You may want to add index pages like `"/tags/clojure"` so you can link to all your pages about Clojure. Nuzzle calls these **tag index pages**. Nuzzle adds both subdirectory and tag index pages automatically for all subdirectories and tags present in your site data.

> You may not want to publish all the index pages that Nuzzle adds to your site data. That's ok! You can avoid publishing a page by returning `nil` from your page rendering function.

These added index pages have an `:nuzzle/index` key with a value that is a set of page entry keys for any pages directly "below" them. For example, if you had page entries with keys of `[:blog-posts :foo]` and `[:blog-posts :bar]`, Nuzzle would add a page entry with a key of `[:blog-posts]` and an `:nuzzle/index` of `#{[:blog-posts :foo] [:blog-posts :bar]}`.

It's worth noting that you can include index page entries in your `nuzzle.edn` site data, just like any other page. You can add content to your index pages by including a `:nuzzle/content` key:

```clojure
;; Nuzzle will append an :nuzzle/index key later if there are any blog posts
{[:blog-posts]
 {:nuzzle/content "markdown/blog-posts-index-blurb.md"
  :nuzzle/title "My Awesome Blog Posts"}}
```

## Creating a Page Rendering Function
All page entries are transformed into Hiccup by a single function. This function takes a single argument (a page map) and returns a vector of Hiccup.

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `nuzzle.hiccup/raw` wrapper function like so: `(raw "<h1>Title</h1>")`.

Here's an example of a page rendering function called `simple-render-page`:
```clojure
(defn layout [title & body]
  [:html [:head [:title title]]
   (into [:body] body)])

(defn simple-render-page
  [{:nuzzle/keys [render-content title] :keys [id] :as _page}]
  (cond
    ;; Decide what the page should look like based on the data in the page map
    (= [] id) (layout title [:h1 "Home Page"] [:a {:href "/about"} "About"])
    (= [:about] id) (layout title [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
    :else nil))
```

The `render-page` function uses the `:id` value to determine what Hiccup to return. This is how a single function can produce Hiccup for every page.

Just because a page exists in your realized site data doesn't mean you have to include it in your static site. If you don't want a page to be published, just return `nil`.

### Accessing Your Site Data with `get-config`
With many static site generators, accessing global data inside markup templates can be painful to say the least. Nuzzle strives to solve this difficult problem elegantly with a function called `get-config`. While realizing your site data, Nuzzle attaches a copy of this function to each page map under the key `:get-config`.

In a word, `get-config` allows us to see the whole world while creating our Hiccup. It has two forms:

1. `(get-config)`: With no arguments, returns the whole realized config map.
2. `(get-config [:blog-posts])`: With one argument, returns value associated with provided config key or throws an exception.

Since `get-config` returns maps from our *realized* site data, all information about the site is at your fingertips. Every page and peripheral map from your site data is always a function call away.

To make things even more convenient, the `get-config` function attaches a copy of itself to each map it returns. This makes it easy to write functions that accept a page or peripheral map without ever having to worry about passing `get-config` along with it. It's kind of like a self-replicating bridge from one config value to the next!

There are many use cases for the `get-config` function. It's great for creating index pages, accessing peripheral maps, and countless other things:

```clojure
(defn unordered-list [& list-items]
  (->> list-items
       (map (fn [item] [:li item]))
       (into [:ul])))

(defn layout [{:nuzzle/keys [title] :keys [get-config] :as _page} & body]
  (let [{:keys [twitter]} (get-config :social)
        {about-url :nuzzle/url} (get-config [:about])]
    [:html [:head [:title title]]
     (into [:body
            [:header
             (unordered-list
              [:a {:href about-url} "About"]
              [:a {:href twitter} "My Tweets"])]]
           body)]))

(defn render-index-page [{:nuzzle/keys [title index] :keys [get-config] :as page}]
  (layout page
          [:h1 (str "Index page for " title)]
          (->>
           (for [id index
                 :let [{:nuzzle/keys [url title]} (get-config id)]]
             [:a {:href url} title])
           (apply unordered-list))))

(defn render-homepage [{:keys [get-config] :as page}]
  (layout page
          [:h1 "Home Page"]
          [:p (str "Hi there, welcome to my website. If you want to read my rants about Clojure, click ")
           [:a {:href (-> [:tags :clojure] get-config :nuzzle/url)} "here!"]]))

(defn render-page [{:nuzzle/keys [render-content title] :keys [id] :as page}]
  (cond
   (= [] id) (render-homepage page)
   (= [:about] id) (layout page [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
   (= [:tags :clojure] id) (render-index-page page)
   :else (layout page [:h1 title] (render-content))))
```

At the bottom of the function we can see the function from `:nuzzle/render-content` being used. Recall that this function will be present in every page map, and it will either return `nil` or some Hiccup that was generated from the associated content file.

## Generating an RSS feed
Nuzzle comes with support for generating an RSS feed. (TODO)

## Syntax Highlighting
Syntax-highlighted code can give your website a polished, sophisticated appearance. Nuzzle let's you painlessly plug your Markdown code-blocks into [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma). Nuzzle uses `clojure.java.sh/sh` to interact with these programs. Since they are not available as Java or Clojure libraries, Nuzzle users must manually install them into their $PATH in order for Nuzzle to use them.

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
