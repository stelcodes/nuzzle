# Nuzzle
A data-oriented micro-framework for quickly creating static sites with Clojure and Hiccup.

## Design Goals
- Simple configuration
- Turn Hiccup into static web pages
- Support for creating subdirectory index pages
- Support for tagging pages and creating tag index pages
- Out-of-the-box hot-reloading development server

## Interface
Nuzzle aims to be extradinarly simple to use. All of Nuzzle's public API is in the `codes.stel.nuzzle.core` namespace. There are only three functions: `start-server`, `inspect` and `export`. All three of these functions accept one argument: the global config map.
- `start-server`: A helper function for development. Starts a `http-kit` server which builds each page from scratch upon each request. Returns a function to stop the server.
- `inspect`: A helper function for development. Returns a modified `site-config` map with all the additions Nuzzle makes before sending the page data to the user's rendering function. These additions include adding the tag index pages, group index pages, and URIs.
- `export`: Exports the static site to disk, creating `:target-dir` if necessary. Copies the contents of `:static-dir` into `:target-dir`.

## Configuration
### Global Config Map
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


### Site Config Map
A site config is an EDN map that is kept in an `.edn` file in your `resources` directory or somwhere else on your classpath.
```clojure
{
  [:about]
  {:title "About"}

  [:blog-posts :using-clojure]
  {:title "Using Clojure"}

  [:blog-posts :learning-rust]
  {:title "Learning Rust"}

  :social
  {:twitter "https://twitter.com/username"}
}
```

