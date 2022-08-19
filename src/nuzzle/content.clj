(ns nuzzle.content
  ;; (:use clojure.stacktrace)
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [clojure.walk :as w]
   [cybermonday.core :as cm]
   [cybermonday.utils :as cu]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.util :as util]))

(defn quickfigure-element
  [[_tag {:keys [src title] :as _attr}]]
  [:figure [:img {:src src}]
   [:figcaption [:h4 title]]])

(defn gist-element
  [[_tag {:keys [user id] :as _attr}]]
  [:script {:type "application/javascript"
            :src (str "https://gist.github.com/" user "/" id ".js")}])

(defn youtube-element
  [[_tag {:keys [title id] :as _attr}]]
  [:div {:style "position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden;"}
   [:iframe {:src (str "https://www.youtube.com/embed/" id)
             :style "position: absolute; top: 0; left: 0; width: 100%; height: 100%; border:0;"
             :title title :allowfullscreen true}]])

(defn render-custom-element
  [[tag & _ :as hiccup]]
  (case tag
    :quickfigure (quickfigure-element hiccup)
    :gist (gist-element hiccup)
    :youtube (youtube-element hiccup)
    hiccup))

(defn walk-hiccup-for-custom-elements
  [hiccup]
  {:pre [(sequential? hiccup)]}
  (if (list? hiccup)
    (map walk-hiccup-for-custom-elements hiccup)
    (w/prewalk
     (fn [item]
       (if (cu/hiccup? item)
         (if (-> item second map?)
           (render-custom-element item)
           (render-custom-element (apply vector (first item) {} (rest item))))
         item))
     hiccup)))

(defn generate-chroma-command
  [file-path language config]
  (let [{:keys [style line-numbers?]} (:nuzzle/syntax-highlighter config)]
    ["chroma" (str "--lexer=" language) "--formatter=html" "--html-only"
     "--html-prevent-surrounding-pre" (when style "--html-inline-styles")
     (when style (str "--style=" style)) (when line-numbers? "--html-lines")  file-path]))

(defn generate-pygments-command
  [file-path language config]
  (let [{:keys [style line-numbers?]} (:nuzzle/syntax-highlighter config)
        ;; TODO: turn nowrap on for everything if they release my PR
        ;; https://github.com/pygments/pygments/issues/2127
        options [(when-not line-numbers? "nowrap") (when style "noclasses")
                 (when style (str "style=" style))
                 (when line-numbers? "linenos=inline")]]
    ["pygmentize" "-f" "html" "-l" language "-O"
     (->> options (remove nil?) (str/join ",")) file-path]))

(defn generate-highlight-command
  [file-path language config]
  (->>
   (case (get-in config [:nuzzle/syntax-highlighter :provider])
    :chroma (generate-chroma-command file-path language config)
    :pygments (generate-pygments-command file-path language config))
   (remove nil?)))

(defn highlight-code [code language config]
  (let [tmp-file (fs/create-temp-file)
        tmp-file-path (-> tmp-file fs/canonicalize str)
        _ (spit tmp-file-path code)
        highlight-command (generate-highlight-command tmp-file-path language config)
        {:keys [exit out err]} (apply util/safe-sh highlight-command)]
    (if (not= 0 exit)
      (do
        (log/warn "Syntax highlighting command failed:" (str/join " " highlight-command))
        (log/warn err)
        code)
      (do
        (fs/delete-if-exists tmp-file)
        out))))

(defn code-block->hiccup [[_tag-name {:keys [language]} code] config]
  (if (and language (:nuzzle/syntax-highlighter config))
    [:pre
     [:code.code-block (hiccup/raw (highlight-code code language config))]]
    [:pre [:code.code-block code]]))

(defn process-markdown-file [file config]
  ;; (print-stack-trace (ex-info "PROCESSING MARKDOWN FILE" {:file file}) 12)
  (let [code-block-with-config #(code-block->hiccup % config)
        lower-fns {:markdown/fenced-code-block code-block-with-config
                   :markdown/indented-code-block code-block-with-config}
        ;; Avoid the top level :div {}
        [_ _ & hiccup] (-> file
                           slurp
                           (cm/parse-body {:lower-fns lower-fns}))]
    (walk-hiccup-for-custom-elements hiccup)))

(defn process-html-file
  [content-file _config]
  (hiccup/raw (slurp content-file)))

(defn create-render-content-fn
  "Create a function that turns the :nuzzle/content file into the correct form for the
  hiccup compiler: vector, list, or raw string"
  [page-key config & {:keys [lazy-render?] :as _opts}]
  {:pre [(or (vector? page-key) (keyword? page-key))]}
  (if-let [content (get-in config [page-key :nuzzle/content])]
    (let [content-file (fs/file content)
          content-ext (fs/extension content-file)]
      (if (fs/exists? content-file)
        (cond
         (#{"md" "markdown"} content-ext) (if lazy-render?
                                            #(process-markdown-file content-file config)
                                            (constantly (process-markdown-file content-file config)))
         (#{"html" "htm"} content-ext) (if lazy-render?
                                         #(process-html-file content-file config)
                                         (constantly (process-html-file content-file config)))
         :else (throw (ex-info (str "Content file " (fs/canonicalize content-file)
                                    " for page " page-key " has unrecognized extension "
                                    content-ext ". Must be one of: md, markdown, html, htm")
                               {:nuzzle/page-key page-key :nuzzle/content content})))
        ;; If markdown-file is defined but it can't be found, throw an Exception
        (throw (ex-info (str "Content file " (fs/canonicalize content-file)
                             " for page " page-key " not found")
                        {:nuzzle/page-key page-key :nuzzle/content content}))))
    ;; If :nuzzle/content is not defined, just make a function that returns nil
    (constantly nil)))
