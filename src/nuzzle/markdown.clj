(ns nuzzle.markdown
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
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

(defn generate-chroma-command
  [file-path language style]
  ["chroma" (str "--lexer=" language) "--formatter=html" "--html-only"
   "--html-inline-styles" "--html-prevent-surrounding-pre"
   (str "--style=" style) file-path])

(defn generate-pygment-command
  [file-path language style]
  ["pygmentize" "-f" "html" "-O" (str "nowrap,noclasses,style=" style)
   "-l" language file-path])

(def highlight-provider-map
  {:chroma generate-chroma-command
   :pygment generate-pygment-command})

(defn highlight-code [code language config]
  (let [{:keys [provider style]} (get-in config [:markdown-opts :syntax-highlighting])
        tmp-file (fs/create-temp-file)
        tmp-file-path (-> tmp-file fs/canonicalize str)
        _ (spit tmp-file-path code)
        highlight-command-fn (get highlight-provider-map provider)
        highlight-command (highlight-command-fn tmp-file-path language style)
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
  (if (and language (get-in config [:markdown-opts :syntax-highlighting]))
    [:code.code-block
     [:pre (hiccup/raw
            (highlight-code code
                            language
                            config))]]
    [:code.code-block [:pre code]]))

(defn process-markdown-file [file config]
  (let [code-block-with-config #(code-block->hiccup % config)
        lower-fns {:markdown/fenced-code-block code-block-with-config
                   :markdown/indented-code-block code-block-with-config}
        ;; Avoid the top level :div {}
        [_ _ & hiccup] (-> file
                           slurp
                           (cm/parse-body {:lower-fns lower-fns}))]
    (walk-hiccup-for-shortcodes hiccup)))

(defn create-render-markdown-fn
  "Create a function that turned the :markdown file into html, wrapped with the
  hiccup raw identifier."
  [id markdown config]
  {:pre [(or (vector? id) (keyword? id)) (or (nil? markdown) (string? markdown))]}
  (if-not markdown
    ;; If :markdown is not defined, just make a function that returns nil
    (constantly nil)
    (let [markdown-file (fs/file markdown)]
      (if markdown-file
        (fn render-markdown []
          (process-markdown-file markdown-file config))
        ;; If markdown-file is defined but it can't be found, throw an Exception
        (throw (ex-info (str "Markdown file " (fs/canonicalize markdown-file) " for id " id " not found")
                        {:id id :markdown markdown}))))))
