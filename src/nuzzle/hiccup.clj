(ns nuzzle.hiccup
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [clojure.walk :as w]
   [nuzzle.hiccup-compiler :as hiccup-compiler]
   [nuzzle.log :as log]
   [nuzzle.util :as util]))

(defn raw-html [raw-html-str]
  [:<> {:dangerouslySetInnerHTML {:__html raw-html-str}}])

(defn hiccup->html [& hiccup]
  (when (seq hiccup)
    (hiccup-compiler/render-html hiccup)))

(comment (hiccup->html (raw-html "<h1>hi<h1>")))

(defn hiccup->html-document [& hiccup]
  (when (seq hiccup)
    (str "<!DOCTYPE html>" (apply hiccup->html hiccup))))

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

(defn highlight-code [hiccup provider & {:as opts}]
  (let [[tag {:keys [class lang] :as attrs} code] hiccup
        class-lang-matcher #(when (string? %)
                              (->> % (re-find #"language-(\S+)") second))
        language (or lang
                     (cond
                      (string? class) (class-lang-matcher class)
                      (coll? class) (some class-lang-matcher class)))]
    (if-not language
      hiccup
      (let [tmp-file (fs/create-temp-file)
            tmp-file-path (-> tmp-file fs/canonicalize str)
            _ (spit tmp-file-path code)
            highlight-command (case provider
                                :chroma (util/generate-chroma-command tmp-file-path language opts)
                                :pygments (util/generate-pygments-command tmp-file-path language opts))
            {:keys [exit out err]} (apply util/safe-sh highlight-command)]
        (if (not= 0 exit)
          (do
            (log/warn "Syntax highlighting command failed:" (str/join " " highlight-command))
            (log/warn err)
            hiccup)
          (do
            (fs/delete-if-exists tmp-file)
            [tag attrs (raw-html out)]))))))

