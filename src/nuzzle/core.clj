(ns nuzzle.core
  (:require
   [cybermonday.core :as cm]
   [nrepl.cmdline :as nrepl-cmdline]
   [nrepl.server :as nrepl]
   [nuzzle.publish :as publish]
   [nuzzle.log :as log]
   [nuzzle.server :as server]))

(defn publish
  "Publishes the website to :nuzzle/publish-dir. The overlay directory is
  overlayed on top of the publish directory after the web pages have been
  published."
  [pages & {:as opts}]
  (publish/publish-site pages opts))

(defn serve
  "Starts a server using http-kit for development."
  [pages & {:as opts}]
  (server/start-server pages opts))

(defn develop
  "Starts the site server and an nREPL server as well for an easy hot-reloading
  setup"
  [pages & {:as opts}]
  (let [nrepl-server (nrepl/start-server)
        stop-site-server (serve pages opts)]
    (nrepl-cmdline/save-port-file nrepl-server {})
    (log/log-nrepl-server (:port nrepl-server))
    (fn [& _] (stop-site-server) (nrepl/stop-server nrepl-server))))

(defn parse-md [md-str]
  (let [lower-code-block
        (fn lower-code-block [[_tag-name {:keys [language]} code]]
          [:pre
           [:code
            {:lang language
             :class ["code-block" (when language (str "language-" language))]}
            code]])
        lower-fns {:markdown/fenced-code-block lower-code-block
                   :markdown/indented-code-block lower-code-block}
        ;; Avoid the top level div [:div {} content...]
        [_ _ & hiccup] (cm/parse-body md-str {:lower-fns lower-fns})]
    hiccup))

