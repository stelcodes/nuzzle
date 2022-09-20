<p align="center">
<img src="./assets/nuzzle2-with-text.svg" alt="Nuzzle logo" width="400">
</p>
<p align="center">
✨ A functional static site generator so smol you won't even notice it's there ✨
</p>
<div align="center">
  <img src="https://github.com/stelcodes/nuzzle/actions/workflows/unit-tests.yaml/badge.svg" alt="unit tests">
  <img src="https://github.com/stelcodes/nuzzle/actions/workflows/clj-kondo.yaml/badge.svg" alt="clj-kondo">
  <a href="https://clojars.org/codes.stel/nuzzle"><img src="https://img.shields.io/clojars/v/codes.stel/nuzzle.svg" alt="Clojars Project"></a>
</div>
<hr>

> **WARNING**: Nuzzle is in the alpha stage. Expect API breakages.

## What is Nuzzle?
Nuzzle is a static site generator packaged as a Clojure library. It requires an astonishingly small code footprint to use. Keep all your static site data and building instructions in a single Clojure file!

## Design Goals
- Easy to use for developers new to Clojure
- Highly flexible for advanced Clojure users
- Great development experience with excellent hot-reloadability story
- Semantically aligned with the [Atom feed spec](https://validator.w3.org/feed/docs/atom.html)
- Small code footprint! Your Clojure code interacting with Nuzzle can be extremely succinct such that you can keep it all in a single file and run Nuzzle functions via a Clojure CLI tool alias
- Users can define their own process for loading content so it can be of any type (Markdown, Org, HTML, etc) and come from anywhere (local files, headless CMS API calls, mix of both, etc)

## Feature List (Nuzzle users can...)
- Generate an Atom feed with embedded HTML content
- Generate a sitemap
- Generate tag index pages from tag information
- Define all markup in hiccup without ever worrying about converting it to HTML
- Retrieve all data for any page while creating markup
- Start dual website/nREPL servers without adding nREPL deps for creating incredible development feedback loop
- Inject a script based on [livejs](https://livejs.com/) with configurable refresh interval to automatically refresh browser view to see content/markup changes in realtime while developing
- Transform hiccup to do any customization imaginable including statically rendering syntax highlighted code blocks with [Pygments](https://github.com/pygments/pygments) or [Chroma](https://github.com/alecthomas/chroma)

## Real World Example
Want to read some code already? Check out [this repo](https://github.com/stelcodes/dev-blog) which uses Nuzzle to build my personal developer blog deployed at [stel.codes](https://stel.codes).

![Pretty logging when running nuzzle.core/develop](https://user-images.githubusercontent.com/22163194/190880734-ff36a238-a00c-4431-a720-5308c07f57a1.png)

## Requirements
- Java >= 11
- Clojure >= 1.11.1

## Usage
Here's a minimal Nuzzle setup:

`deps.edn`
```clojure
{{examples/minimal/deps.edn}}
```

`site.clj`
```clojure
{{examples/minimal/site.clj}}
```

Call the `site/develop` or `site/publish` functions from the command line:
```bash
# Using Babashka (my preference)
bb clojure -T:site develop
bb clojure -T:site publish

# Using official Clojure CLI executable
clj -T:site develop
clj -T:site publish
```

> Test it out yourself! `git clone https://github.com/stelcodes/nuzzle && cd nuzzle/examples/minimal`

## Ash Ketchum Example
If you're a trainer from Pallet town, your `site.clj` might look like this:

```clojure
{{examples/ash-ketchum-blog/site.clj}}
```
> Test it out yourself! `git clone https://github.com/stelcodes/nuzzle && cd nuzzle/examples/ash-ketchum-blog`

## Page Entries

Each page entry in the pages map represents a single page of the static site. Each entry must have a key that is a **vector of keywords** representing a relative URL. The key `[:blog-posts :catching-pikachu]` translates to the URL `"/blog-posts/catching-pikachu"` and will be rendered to disk as `<publish-dir>/blog-posts/catching-pikachu/index.html`. The associated value must be a map with data about the page. The keys of the page are based on the [Atom feed spec](https://validator.w3.org/feed/docs/atom.html).

```clojure
{{doc/page-entry.clj}}
```

## Creating `:nuzzle/render-page`
Each function under the `:nuzzle/render-page` key must turn that page map into Hiccup. The function must take one parameter (a page entry map). It can return a vector of Hiccup (more flexible) or a string of HTML (wrapped with `nuzzle.hiccup/raw-html`)).

> In Nuzzle, all strings in the Hiccup are automatically escaped, so if you want to add a string of raw HTML, use the `nuzzle.hiccup/raw-html` wrapper function like so: `(raw-html "<h1>Title</h1>")`.

> Hiccup is a method for representing HTML using Clojure data-structures. It comes from the original [Hiccup library](https://github.com/weavejester/hiccup) written by [James Reeves](https://github.com/weavejester). For a quick guide to Hiccup, check out this [lightning tutorial](https://medium.com/makimo-tech-blog/hiccup-lightning-tutorial-6494e477f3a5).

## Using `:nuzzle/get-pages`
With many static site generators, accessing global data inside markup templates can be *painful*. Since Nuzzle is just playing with data and functions, this problem becomes much easier to solve.

Instead of complicating your page rendering functions with multiple required arguments, Nuzzle adds a function to each page entry map under the key `:nuzzle/get-pages`. This function can access the whole pages map, making referencing other pages a breeze.

```clojure
;; Get a list of every page
(get-pages)
;; Get a single page
(get-pages [:blog-posts])
;; Get a list of all the direct children of a page
(get-pages [:blog-posts] :children? true)
```

The `get-pages` function will always return pages which also have the `:nuzzle/get-pages` key attached. This naturally lends itself to a convention where most Hiccup-generating functions can accept a page entry map as its first or only argument while still being able to access any data in your whole site if need be.
