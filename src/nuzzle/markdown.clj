(ns nuzzle.markdown
  (:require
   [babashka.fs :as fs]
   [clojure.walk :as w]
   [cybermonday.core :as cm]
   [cybermonday.utils :as cu]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.util :as util]))

(defn quickfigure-shortcode
  [{:keys [src title]}]
  [:figure [:img {:src src}]
   [:figcaption [:h4 title]]])

(defn gist-shortcode
  [{:keys [user id]}]
  [:script {:type "application/javascript"
            :src (str "https://gist.github.com/" user "/" id ".js")}])

(defn youtube-shortcode
  [{:keys [title id]}]
  [:div {:style "position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden;"}
   [:iframe {:src (str "https://www.youtube.com/embed/" id)
             :style "position: absolute; top: 0; left: 0; width: 100%; height: 100%; border:0;"
             :title title :allowfullscreen true}]])

(def shortcode-map
  {:quickfigure quickfigure-shortcode
   :gist gist-shortcode
   :youtube youtube-shortcode})

(defn render-shortcode
  [[tag attr & _ :as hiccup]]
  (if-let [shortcode-fn (get shortcode-map tag)]
    (shortcode-fn attr)
    hiccup))

(defn walk-hiccup-for-shortcodes
  [hiccup]
  {:pre [(sequential? hiccup)]}
  (if (list? hiccup)
    (map walk-hiccup-for-shortcodes hiccup)
    (w/prewalk
     (fn [item]
       (if (cu/hiccup? item)
         (render-shortcode item)
         item))
     hiccup)))

(defn highlight-code [highlight-style language code]
  (let [code-file (fs/create-temp-file)
        code-path (str (fs/canonicalize code-file))
        _ (spit code-path code)
        chroma-command ["chroma" (str "--lexer=" language) "--formatter=html" "--html-only"
                        "--html-inline-styles" "--html-prevent-surrounding-pre"
                        (str "--style=" highlight-style) code-path]
        {:keys [exit out err]} (util/safe-sh chroma-command)]
    (if (not= 0 exit)
      (do
        (log/warn "Failed to highlight code:" code-path)
        (log/warn err)
        code)
      (do
        (fs/delete-if-exists code-file)
        out))))

(defn code-block-highlighter [highlight-style [_tag-name {:keys [language]} body]]
  (if highlight-style
    [:code.code-block
     [:pre (hiccup/raw
            (highlight-code highlight-style
                            (or language "no-highlight")
                            body))]]
    [:code.code-block [:pre body]]))

(defn process-markdown-file [highlight-style file]
  (let [code-block-with-style (partial code-block-highlighter highlight-style)
        lower-fns {:markdown/fenced-code-block code-block-with-style
                   :markdown/indented-code-block code-block-with-style}
        ;; Avoid the top level :div {}
        [_ _ & hiccup] (-> file
                           slurp
                           (cm/parse-body {:lower-fns lower-fns}))]
    (walk-hiccup-for-shortcodes hiccup)))

(defn create-render-markdown-fn
  "Create a function that turned the :markdown file into html, wrapped with the
  hiccup raw identifier."
  [id markdown {:keys [highlight-style]}]
  {:pre [(or (vector? id) (keyword? id)) (or (nil? markdown) (string? markdown))]}
  (if-not markdown
    ;; If :markdown is not defined, just make a function that returns nil
    (constantly nil)
    (let [markdown-file (fs/file markdown)]
      (if markdown-file
        (fn render-markdown []
          (process-markdown-file highlight-style markdown-file))
        ;; If markdown-file is defined but it can't be found, throw an Exception
        (throw (ex-info (str "Markdown file " (fs/canonicalize markdown-file) " for id " id " not found")
                        {:id id :markdown markdown}))))))
