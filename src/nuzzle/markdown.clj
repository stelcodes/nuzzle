(ns nuzzle.markdown
  (:require
   [babashka.fs :as fs]
   [cybermonday.core :as cm]
   [clojure.string :as string]
   [nuzzle.hiccup :as hiccup]
   [nuzzle.log :as log]
   [nuzzle.util :as util]))

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
    [:code [:pre (hiccup/raw
                  (highlight-code highlight-style
                                  (or language "no-highlight")
                                  body))]]
    [:code [:pre body]]))

(defn process-markdown-file [highlight-style file]
  (let [code-block-with-style (partial code-block-highlighter highlight-style)
        lower-fns {:markdown/fenced-code-block code-block-with-style
                   :markdown/indented-code-block code-block-with-style}
        [_ _ & hiccup] ; Avoid the top level :div {}
        (-> file
            slurp
            (cm/parse-body {:lower-fns lower-fns}))]
    (hiccup/html hiccup)))

(defn create-render-content-fn
  "Create a function that turned the :content file into html, wrapped with the
  hiccup raw identifier."
  [id content {:keys [highlight-style]}]
  {:pre [(vector? id) (or (nil? content) (string? content))]}
  (if-not content
    ;; If :content is not defined, just make a function that returns nil
    (constantly nil)
    (if-let [content-file (fs/file content)]
      (let [ext (fs/extension content-file)]
        (cond
         ;; If a html or svg file, just slurp it up
         (or (= "html" ext) (= "svg" ext))
         (fn render-html []
           (hiccup/raw (string/trim (slurp content-file))))
         ;; If markdown, convert to html
         (or (= "markdown" ext) (= "md" ext))
         (fn render-markdown []
           (hiccup/raw (process-markdown-file highlight-style content-file)))
         ;; If extension not recognized, throw Exception
         :else (throw (ex-info (str "Filetype of content file " content " for id " id " not recognized")
                      {:id id :content content}))))
      ;; If content-file is defined but it can't be found, throw an Exception
      (throw (ex-info (str "Resource " content " for id " id " not found")
                      {:id id :content content})))))
