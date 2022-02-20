# Nuzzle
A data-oriented micro-framework for quickly creating static sites with Clojure and Hiccup.

## Design Goals
- Simple configuration
- Hot-reloading development experience

## Configuration
### Global Config Map
Nuzzle has a global configuration map:
```clojure
{
  :site-config <site-config-map>
  :remove-drafts? <boolean>
  :render-fn <function>
}
```

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

## Interface
Nuzzle aims to be extradinarly simple to use. All of Nuzzle's public API is in the `codes.stel.nuzzle.core` namespace. There are only three functions: `inspect`, `export` and `start-server`. All three of these functions accept the same global config map.
- `inspect`: This function takes a global config map and returns a site config map that 

