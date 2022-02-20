# Nuzzle ✨🐈
A data-oriented micro-framework for quickly creating static sites with Clojure and Hiccup.

## Design Goals
- Simple configuration
- Turn Hiccup into static web pages
- Support for subdirectory index pages
- Support for tagging pages and tag index pages
- Out-of-the-box hot-reloading development server

## Interface
Nuzzle aims to be extradinarly simple to use. All of Nuzzle's public API is in the `codes.stel.nuzzle.core` namespace. There are only three functions: `start-server`, `inspect` and `export`. All three of these functions accept one argument: the `global-config` map.
- `start-server`: A helper function for development. Starts a `http-kit` server which builds each page from scratch upon each request. Returns a function to stop the server.
- `inspect`: A helper function for development. Returns a modified `site-config` map with all the additions Nuzzle makes before sending the page data to the user's rendering function. These additions include adding the tag index pages, group index pages, and URIs.
- `export`: Exports the static site to disk, creating `:target-dir` if necessary. Copies the contents of `:static-dir` into `:target-dir`.

## Configuration
### `global-config`
Nuzzle has a simple global configuration map:
```clojure
{
  :site-config <path>
  :remove-drafts? <boolean>
  :render-fn <function>
  :static-dir <path>
  :target-dir <path>
}
```
- `:site-config`: a path to an `.edn` resource on the classpath. Must be a map conforming to the `site-config` spec. This map defines the structure of the static site and is essential to all `nuzzle` functionality.
- `:remove-drafts?`: a boolean that indicates whether pages in the `site-config` map with `:draft? true` should be removed.
- `:render-fn`: a function that takes a single argument (map) and returns either `nil` or a Hiccup vector.
- `:static-dir`: a path to a resource directory on the classpath that contains static assets that should be copied into the exported site.
- `:target-dir`: a path to the directory that the site should be exported to. This path does not have to be on the classpath. Defaults to `dist`.
- `:dev-port`: a port number for the development server to listen on. Defaults to 5868.


### `site-config`
A site config is an EDN map that is kept in an `.edn` file in your `resources` directory or somwhere else on your classpath.

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
  {:title "Learning Rust"
   :resource "markdown/learning-rust.md"
   :tags [:rust]}

  [:blog-posts :how-to-install-fedora]
  {:title "How to Install Fedora"
   :resource "markdown/how-to-install-fedora.md"
   :tags [:linux :fedora]
   :draft? true}

  :social
  {:twitter "https://twitter.com/username"}
}
```

Each key in this map is called an `id`. Each `id` can either be a keyword or a vector of keywords.

If the `id` is a keyword like `:social`, the key-value pair is just extra information about the site. It has no effect on the website structure. It can easily be retrieved in the `render` function.

If the `id` is a vector like `[:blog-posts :using-clojure]`, it represents a single page of the web site. For example, the key `[:about]` translates to the URI `"/about"` and the key `[:blog-posts :using-clojure]` translates to the URI `"/blog-posts/using-clojure"`. From now on we'll call these values **pages**. Pages have some special keys:

- `:resource`: a path to a resource file on the classpath that contains the page's contents. Pages with a `:resource` key will have another key added called `:render-resource` whose value is a function that returns a string of raw html. Supported `:resource` filetypes are `.html`, `.md`, and `.markdown`.
- `:tags`: a vector of keywords. Nuzzle takes the tags of all pages and adds tag index pages to the `site-config`.
- `:draft?`: a boolean indicating whether this page is a draft or not. This key is used when the `global-config` has `:remove-drafts?` set to `true`.

## Rendering
The function defined in the `:render-fn` field in the `global-config` is the single function responsible for producing the Hiccup for every page in the static site. The `id` of every page is attached the page under the key `:id`.

Here's an extremely simple rendering function:
```clojure
(defn render [{:keys [id title render-resource] :as page}]
  (cond
    (= [] id) [:html [:h1 "Home Page"]]
    (= [:about] id) [:html [:h1 "About Page"]]
    :else [:html [:h1 title] (render-resource)]))
```
