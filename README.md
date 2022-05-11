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
- The awesome combination of Clojure and Hiccup 💞
- Simplicity 🌷

Nuzzle is a Clojure library, but you could also think about it as a micro-framework. It's goal is to define a simple yet powerful process for turning data and functions into a static site.

**With Nuzzle you can...**
- Manage all website data and structure inside an EDN file
- Plug in a single function to produce Hiccup
- Retrieve all website data while inside that function
- Statically render CSS syntax highlighting for Markdown code-blocks (requires either [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma))
- Utilize a built-in, REPL-driven, hot-reloading web server
- Tag webpages
- Create subdirectory and tag index webpages
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
- `export`: Exports the static site to disk.
- `serve`: Starts a web server (http-kit) for a live preview of the website, building each webpage from scratch upon each request.
- `realize`: Helper function for visualizing your site data after Nuzzle's additions.

All three functions have exactly the same interface:
- They require no arguments.
- They accept keyword arguments which you can use to override the values in your configuration file.

## Configuration File
Nuzzle expects to find an EDN map in the file `nuzzle.edn` in your current working directory containing these keys:

- `:location` - URI where site will be hosted. Must start with "http://" or "https://". Required.
- `:site-data` - A set of maps describing the structure and content of your website. Required.
- `:render-webpage` - A fully qualified symbol pointing to your webpage rendering function. Required.
- `:export-dir` - A path to a directory to export the site into. Defaults to `"out"`.
- `:overlay-dir` - A path to a directory that will be overlayed on top of `:export-dir` as the final stage of exporting. Defaults to `nil` (no overlay).
- `:markdown-opts` - A map of markdown processing options (syntax highlighting, shortcodes)
- `:rss-channel` - A map with an RSS channel specification. Defaults to nil (no RSS feed).
- `:remove-drafts?` - A boolean that indicates whether webpages marked as a draft should be removed. Defaults to nil (no draft removal).
- `:server-port` - A port number for the development server to listen on. Defaults to 6899.

If you're from Pallet town, your `nuzzle.edn` config might look like this:
```clojure
{:location "https://ashketchum.com"
 :overlay-dir "overlay"
 :render-webpage views/render-webpage
 :site-data
 #{{:id []
    :content "markdown/homepage-introduction.md"}
   {:id [:blog-posts :catching-pikachu]
    :title "How I Caught Pikachu"
    :content "markdown/how-i-caught-pikachu.md"}
   {:id [:blog-posts :defeating-misty]
    :title "How I Defeated Misty with Pikachu"
    :content "markdown/how-i-defeated-misty.md"}
   {:id [:about]
    :content "markdown/about-ash.md"}
   {:id :crypto
    :bitcoin "1GVY5eZvtc5bA6EFEGnpqJeHUC5YaV5dsb"
    :eth "0xc0ffee254729296a45a3885639AC7E10F9d54979"}}}
```

## Site Data
The `:site-data` value defines the attributes of all the static site's webpages as well as any supplemental information. Site data must be a set of maps, and those maps have just one required key: `:id`.

If the `:id` is a **vector of keywords**, the map represents a typical **webpage**. The `:id` `[:blog-posts :catching-pikachu]` translates to the URI `"/blog-posts/catching-pikachu"` and will be rendered to disk as `<export-dir>/blog-posts/catching-pikachu/index.html`. Nuzzle calls these *webpage maps*.

If the `:id` is a singular **keyword**, the map just contains extra information about the site. It has no effect on the website structure. Nuzzle calls these *peripheral maps*. The last map in the example above with the `:id` of `:crypto` is a peripheral map.

Here's another `:site-data` example with annotations:
```clojure
#{
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Webpage Maps

  ;; The homepage
  {:id []          ; <- This represents the URI "/"
   :title "Home"}

  {:id [:about]    ; <- This represents the URI "/about"
   :title "About"} ; <- Add a title if you'd like

  {:id [:blog-posts :using-clojure]
   :title "Using Clojure"
   ;; The special :content key associates a Markdown or HTML file
   :content "markdown/using-clojure.md"
   ;; The special :tags key tells Nuzzle about webpage tags
   :tags #{:clojure}}

  {:id [:blog-posts :learning-rust]
   :title "How I Got Started Learning Rust"
   :content "markdown/learning-rust.md"
   :tags #{:rust}
   ;; The special :draft? key tells Nuzzle which webpages are drafts
   :draft? true
   ;; The special :rss key tells Nuzzle to include the webpage in the RSS XML file
   :rss? true}

  {:id [:blog-posts :clojure-on-fedora]
   :title "How to Install Clojure on Fedora"
   :content "markdown/clojure-on-fedora.md"
   :tags #{:linux :clojure}
   ;; Webpage maps are open, you can include any data you like
   :foobar "baz"}

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Peripheral Maps

  ;; Extra information not particular to any webpage
  {:id :social
   :twitter "https://twitter.com/clojurerulez"} ; <- This will be easy to retrieve later

  {:id :footer-message
   ;; You can also associate content with peripheral maps
   :content "markdown/footer-message.md"}
}
```

### Special Keys in Webpage Maps
- `:content`: A path to an associated Markdown or HTML file.
- `:tags`: A set of keywords where each keyword is a tag name.
- `:draft?`: A boolean indicating whether this webpage is a draft or not.
- `:rss?`: A boolean indicating whether the webpage should be included in the optional RSS feed.

### Special Keys in Peripheral Maps
- `:content`: A path to an associated Markdown or HTML file.

## How Nuzzle Adds to Your Site Data
You can think of Nuzzle's core functionality as a data pipeline. Nuzzle starts with the site data in your `nuzzle.edn`, adds some spice, sends it through your webpage rendering function, and exports the results to disk.

Nuzzle's site data pipeline can be visualized like so:
```
   ┌───────────┐           ┌───────────┐               ┌──────────────┐
   │           │           │           │               │              │
   │ Site data │ ────┬───► │ Realized  │ ─────┬─────►  │  Hiccup that │
   │   from    │     │     │ site data │      │        │ is converted │
   │ nuzzle.edn│     │     │           │      │        │ to html and  │
   │           │           │           │               │   exported   │
   └───────────┘  Nuzzle   └───────────┘render-webpage │              │
                 additions                 function    └──────────────┘
```

A key part of this process is the first arrow: Nuzzle's additions. Nuzzle calls this **realizing** your site data. It's important to note that Nuzzle does not modify the site data, it only adds to it. The realized site data looks just like the original, but with extra webpage maps and extra keys in the existing maps.

### Adding Keys to Webpage Maps
Nuzzle adds these keys to every webpage map:
- `:uri`: the path of the webpage from the website's root without the `index.html` part (ex `"/blog-posts/learning-clojure/"`).
- `:render-content`: A function that renders the webpage's associated content file if `:content` key is present, otherwise returns `nil`.
- `:get-site-data`: A function that allows you to freely reach into your site data from inside of your webpage rendering function.

### Adding Keys to Peripheral Maps
Nuzzle adds these keys to every peripheral map:
- `:render-content`: Same as above.
- `:get-site-data`: Same as above.

### Adding Webpage Maps (Index Webpages)
Often people want to create index webpages in static sites which serve as a webpage that links to other webpages which share a common trait. For example, if you have webpages like `"/blog-posts/foo"` and `"/blog-posts/bar"`, you may want a webpage at `"/blog-posts"` that links to `"/blog-posts/foo"` and `"/blog-posts/bar"`. Nuzzle calls these *subdirectory index webpages*. Another common pattern is associating tags with webpages. You may want to add index pages like `"/tags/clojure"` so you can link to all your webpages about Clojure. Nuzzle calls these *tag index webpages*. Nuzzle adds both subdirectory and tag index webpages automatically for all subdirectories and tags present in your site data.

> You may not want to export all the index webpages that Nuzzle adds to your site data. That's ok! You can control which webpages get exported inside your webpage rendering function.

What makes these index webpage maps special? They have an `:index` key with a value that is a set of webpage map `id`s for any webpages directly below them. For example, if you had webpage maps with `id`s of `[:blog-posts :foo]` and `[:blog-posts :bar]`, Nuzzle would add a webpage map with an `:id` of `[:blog-posts]` and an `:index` of `#{[:blog-posts :foo] [:blog-posts :bar]}`.

It's worth noting that you can include index webpage maps in your `nuzzle.edn` site data, just like any other webpage. You can add content to your index pages by including a `:content` key:

```clojure
;; Somwhere in your :site-data set...
;; Nuzzle will append an :index value later if there are any blog posts
{:id [:blog-posts]
 :content "markdown/blog-posts-index-blurb.md"
 :title "My Awesome Blog Posts"}
```

## Creating a Webpage Rendering Function
All webpage maps are transformed into Hiccup by a single function. This function takes a single argument (a webpage map) and returns a vector of Hiccup.

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `nuzzle.hiccup/raw` wrapper function like so: `(raw "<h1>Title</h1>")`.

Here's an example of a webpage rendering function called `simple-render-webpage`:
```clojure
(defn layout [title & body]
  [:html [:head [:title title]]
   (into [:body] body)])

(defn simple-render-webpage [{:keys [id title render-content] :as webpage}]
  (cond
    ;; Decide what the webpage should look like based on the data in the webpage map
    (= [] id) (layout title [:h1 "Home Page"] [:a {:href "/about"} "About"])
    (= [:about] id) (layout title [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
    :else nil))
```

The `render-webpage` function uses the `:id` value to determine what Hiccup to return. This is how a single function can produce Hiccup for every webpage.

Just because a webpage exists in your realized site data doesn't mean you have to include it in your static site. If you don't want a webpage to be exported, just return `nil`.

### Accessing Your Site Data with `get-site-data`
With many static site generators, accessing global data inside markup templates can be painful to say the least. Nuzzle strives to solve this difficult problem elegantly with a function called `get-site-data`. While realizing your site data, Nuzzle attaches a copy of this function to each webpage map under the key `:get-site-data`.

In a word, `get-site-data` allows us to see the whole world while creating our Hiccup. It has two forms:

1. `(get-site-data)`: With no arguments, returns the whole realized site data set.
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

(defn render-webpage [{:keys [id title render-content] :as webpage}]
  (cond
   (= [] id) (render-homepage webpage)
   (= [:about] id) (layout webpage [:h1 "About Page"] [:p "nuzzle nuzzle uwu :3"])
   (= [:tags :clojure] id) (render-index-webpage webpage)
   :else (layout webpage [:h1 title] (render-content))))
```

At the bottom of the function we can see the function from `:render-content` being used. Recall that this function will be present in every webpage map, and it will either return `nil` or some Hiccup that was generated from the associated content file.

## Generating an RSS feed
Nuzzle comes with support for generating an RSS feed. (TODO)

## Getting Fancy with Markdown
You can configure how Nuzzle interprets your Markdown files with the top-level `:markdown-opts` map which has these keys:
- `:syntax-highlighting` - A map of syntax highlighting options for code-blocks. Defaults to `nil` (no syntax highlighting).

Here's an example:
```
{:syntax-highlighting
 {:provider :pygments
  :style "algol_nu"}}
```

### Syntax Highlighting
Syntax-highlighted code can give your website a polished, sophisticated appearance. Nuzzle let's you painlessly plug your Markdown code-blocks into [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma). Nuzzle uses `clojure.java.sh/sh` to interact with these programs. Since they are not available as Java or Clojure libraries, Nuzzle users must manually install them into their $PATH in order for Nuzzle to use them.

Syntax highlighting is controlled via the `:syntax-highlighting` map which has these keys:
- `:provider` - A keyword specifying the program to use (either `:chroma` or `:pygments`). Required.
- `:style` - A string specifying a style for Markdown code syntax highlighting. Defaults to `nil` (HTML classes only).
- `:line-numbers?` - A boolean indicating whether line numbers should be included.

When this map is present, Nuzzle will run you code-blocks with language annotations through the chosen syntax-highlighting program. If the program supports the language found in the language annotation (check their lexers list), it will output the code as HTML with the different syntax tokens wrapped in `span` elements.

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
