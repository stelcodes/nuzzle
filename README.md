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
(require '[nuzzle.api :as nuzz)

;; Create a pages map
(defn pages {[]
             {:nuzzle/title "Homepage"
              :nuzzle/render-page (fn [{:nuzzle/keys [title] :as _page}]
                                    [:html
                                     [:h1 title]
                                     [:a {:href [:about]}] "About")}
             [:about]
             {:nuzzle/title "About"
              :nuzzle/render-content #(-> "md/about.md" slurp nuzz/parse-md)
              :nuzzle/render-page (fn [{:nuzzle/keys [render-content title] :as _page}]
                                    [:html
                                     [:h1 title]
                                     (render-content)])}})

;; Start development server
;; Pass the pages as a var to get awesome hot-reloading capabilities!
;; The returned value is a function that stops the server.
(nuzz/serve #'pages)

;; Publish the static site, returns nil
(nuzz/publish pages)
```

## Pages Map
Nuzzle uses a map to model a static site where every key is a URL and every value is a map of details about the page. The pages map is validated by `clojure.spec`. You can find the [pages spec here](https://github.com/stelcodes/nuzzle/blob/main/src/nuzzle/schemas.clj).

If you're from Pallet town, your pages might look like this:
```clojure
(ns user
  (:require
   [nuzzle.api :as nuzz]))

(defn render-page [{:keys [title index render-content get-pages] :as _page}]
 [:html
  [:head [:title title]
         ;; Add link to CSS file /css/main.css (must be in overlay directory)
         [:link {:href "/css/main.css" :rel "stylesheet"}]]
  [:body
   [:h1 title]
   (when index
     [:ul (for [url index
                :let [{:keys [title url]} (get-pages url)]]
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

(serve #'pages :port 8080)

;; Build this site with a sitemap and Atom feed
;; Overlay the directory containing the css/main.css file.

(publish pages :base-url "https://ashketchum.com"
               :atom-feed {:title "Ash Ketchum's Blog"
                           :subtitle "In a world we must defend"}
               :overlay-dir "public")
```

## Page Entries

Each page entry in the pages map represents a single page of the static site. Each entry must have a key that is a **vector of keywords** representing a relative URL. The key `[:blog-posts :catching-pikachu]` translates to the URL `"/blog-posts/catching-pikachu"` and will be rendered to disk as `<publish-dir>/blog-posts/catching-pikachu/index.html`. The associated value must be a map with data about the page. The keys of the page are based on the [Atom feed spec](https://validator.w3.org/feed/docs/atom.html).

```clojure
;; An entry for the page "/blog/the-best-thing-about-clojure/index.html"
[:blog :learning-clojure]

{;; A title for the web page
 ;; Required, no default
 :nuzzle/title "Learning Clojure"

 ;; A function that takes the page map and returns Hiccup containing the whole HTML document
 ;; Required, no default
 :nuzzle/render-page (fn [{:nuzzle/keys [title render-content] [:html [:head [:title title]] [:h1 title] (render-content)])

 ;; A function that takes the page map and returns Hiccup containing the page's main content
 ;; Optional, defaults to (constantly nil)
 :nuzzle/render-content (fn [_page] [:p "The first step to learning Clojure is pressing the ( key."])

 ;; A boolean indicating whether this page is a draft or not
 ;; Optional, defaults to nil
 :nuzzle/draft? false

 ;; A boolean indicating whether the page should be included in the optional Atom feed when publishing
 ;; Optional, defaults to nil
 :nuzzle/feed? true

 ;; A set of keywords where each keyword represents a tag name
 ;; Optional, defaults to nil
 :nuzzle/tags #{:clojure}

 ;; An inst representing when the page was last updated
 ;; Optional, defaults to nil
 :nuzzle/updated #inst "2022-09-16T12:00:00Z"

 ;; A string summary of the page content
 ;; Optional, defaults to nil
 :nuzzle/summary "An in-depth guide to learning Clojure."

 ;; A map representing the author of the page.
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
The `:nuzzle/tag` page keys are used to generate tag pages by using the `nuzzle.api/add-tag-pages` function. This function requires a `:nuzzle/render-page` function for the tag pages.

```clojure
(-> {[:blog :foo]
     {:nuzzle/title "Foo"
      :nuzzle/render-page render-generic-page
      :nuzzle/tags #{:bar :baz}}
     [:about]
     {:nuzzle/title "About"
      :nuzzle/render-page render-about-page}}
    (nuzz/add-tag-pages my-tag-page-render-fn)
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

## Creating a Page Rendering Function
Pages are turned into and HTML string or Hiccup with the function kept under the `:nuzzle/render-page` key in each page map. The function must take one parameter (a page entry map). It can return a vector of Hiccup (more flexible) or a string of HTML (less flexible but works just fine).

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `nuzzle.hiccup/raw` wrapper function like so: `(raw "<h1>Title</h1>")`.

### Accessing the pages with `get-pages`
With many static site generators, accessing global data inside markup templates can be *painful*. Since Nuzzle is heavily data-oriented, this problem becomes much easier to solve.

Instead of requiring your page rendering function to accept multiple arguments, Nuzzle adds a function to each page entry map passed to your page rendering function under the key `:nuzzle/get-pages`. This function can access the whole pages map, making referencing other pages a breeze.

```clojure
;; Get a single page
(get-pages [:blog-posts])
;; Get the whole pages map
(get-pages :all)
```

The `get-pages` function will always return enriched pages which also have the `:nuzzle/get-pages` key attached. This naturally lends itself to a convention where most Hiccup-generating functions can accept a page entry map as its first or only argument while still being able to access any data in your whole site if need be.

## Syntax Highlighting
Syntax-highlighted code can give your website a polished, sophisticated appearance. Nuzzle let's you painlessly plug your Markdown code-blocks into [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma). Nuzzle uses `clojure.java.shell` to interact with these programs. Since they are not available as Java or Clojure libraries, Nuzzle users must manually install them into their $PATH in order for Nuzzle to use them.

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
