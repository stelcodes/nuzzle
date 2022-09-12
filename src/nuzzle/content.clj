(ns nuzzle.content
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [clojure.walk :as w]
   [cybermonday.core :as cm]
   [nuzzle.log :as log]
   [nuzzle.util :as util]))

(defn quickfigure
  [[_tag {:keys [src alt] :as _attr}]]
  [:figure [:img {:src src :alt alt}]
   [:figcaption [:h4 alt]]])

(defn gist
  [[_tag {:keys [user id] :as _attr}]]
  [:script {:type "application/javascript"
            :src (str "https://gist.github.com/" user "/" id ".js")}])

(defn youtube
  [[_tag {:keys [title id] :as _attr}]]
  [:div {:style "position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden;"}
   [:iframe {:src (str "https://www.youtube.com/embed/" id)
             :style "position: absolute; top: 0; left: 0; width: 100%; height: 100%; border:0;"
             :title title :allowfullscreen true}]])

(defn transform-hiccup
  [hiccup transformations]
  (w/postwalk
   (fn [item]
     (if-let [transformation-fn (and (vector? item) (get transformations (first item)))]
       (if (-> item second map?)
         (transformation-fn item)
         (transformation-fn (apply vector (first item) {} (rest item))))
       item))
   hiccup))

(defn generate-chroma-command
  [file-path language & {:keys [style line-numbers?]}]
  (remove nil?
          ["chroma" (str "--lexer=" language) "--formatter=html" "--html-only"
           "--html-prevent-surrounding-pre" (when style "--html-inline-styles")
           (when style (str "--style=" style)) (when line-numbers? "--html-lines")  file-path]))

(defn generate-pygments-command
  [file-path language & {:keys [style line-numbers?]}]
  (let [;; TODO: turn nowrap on for everything if they release my PR
        ;; https://github.com/pygments/pygments/issues/2127
        options (remove nil?
                        [(when-not line-numbers? "nowrap") (when style "noclasses")
                         (when style (str "style=" style))
                         (when line-numbers? "linenos=inline")])]
    ["pygmentize" "-f" "html" "-l" language "-O" (str/join "," options) file-path]))

(defn highlight-code [hiccup provider & opts]
  (let [[tag {classes :class :as attrs} code] hiccup
        language-matcher #(->> % (re-find #"language-(\S+)") second)
        language (if (coll? classes)
                   (some language-matcher classes)
                   (language-matcher classes))]
    (if-not language
      hiccup
      (let [tmp-file (fs/create-temp-file)
            tmp-file-path (-> tmp-file fs/canonicalize str)
            _ (spit tmp-file-path code)
            highlight-command (case provider
                                :chroma (generate-chroma-command tmp-file opts)
                                :pygments (generate-pygments-command tmp-file language opts))
            {:keys [exit out err]} (apply util/safe-sh highlight-command)]
        (if (not= 0 exit)
          (do
            (log/warn "Syntax highlighting command failed:" (str/join " " highlight-command))
            (log/warn err)
            hiccup)
          (do
            (fs/delete-if-exists tmp-file)
            [tag attrs out]))))))

(defn md->hiccup [md-str]
  (let [lower-code-block
        (fn lower-code-block [[_tag-name {:keys [language]} code]]
          [:pre
           [:code
            {:class ["code-block" (when language (str "language-" language))]}
            code]])
        lower-fns {:markdown/fenced-code-block lower-code-block
                   :markdown/indented-code-block lower-code-block}
        ;; Avoid the top level div [:div {} content...]
        [_ _ & hiccup] (cm/parse-body md-str {:lower-fns lower-fns})]
    hiccup))
