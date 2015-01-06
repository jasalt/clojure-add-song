(ns add-song.spotify.api
  "Spotify API stuff"
  (:require
   [clj-http.client        :as client]
   [clojure.data.json      :as json]
   [environ.core           :refer [env]]
   [ring.adapter.jetty :as jetty]
   [clj-time.local :as l]
   clojure.pprint

   ))


;; Load environment variables from ~/.lein/profiles.clj with lein-environ
;; leiningen plugin
(def spotify-client-id (env :spotify-client-id))
(def spotify-client-secret (env :spotify-client-secret))


(defn search-tracks
  "Search track from public Spotify web api"
  [artist title]
  (-> (client/get
       (str "http://ws.spotify.com/search/1/track.json?q="
            (str artist " " title)))
      (:body) (json/read-str :key-fn keyword))
  )

(defn auth-request
  "TODO First authorization step described in
  https://developer.spotify.com/web-api/authorization-guide/"
  []
  ;; OAuth dance, store tokens, auto refresh expiring tokens
  (def resp1 (client/get "https://accounts.spotify.com/authorize"
                         {:query-params {:client_id spotify-client-id
                                         :response_type "code"
                                         :redirect_uri "https://github.com/jasalt/clojure-add-song"
                                         ;;:state opt
                                         :scope "playlist-read-private playlist-modify-private playlist-modify-public"
                                         }}))
  ;;resp1
  ;;(keys resp1)
  ;;(resp1 :status)
  ;;(spit "spotify.html" (resp1 :body))
  ;;(slurp "spotify.html")
  )

(defn run-server
  []
  (defn app [request]

    (def server-log-file (clojure.java.io/writer "server.log" :append true))
    (defn log-server
      [what]
      (binding [*out* server-log-file]
        (println "" )
        (println "--log-entry--" (str (l/local-now)))

        (clojure.pprint/pprint what)
        )
      )

    (log-server request)

    (if (= (request :uri) "/callback")
      (do (log-server "CALLBACK!")
          ;; Continue with callback response
          (.stop server)
          )
      (do
        ;;TODO
        {:status 200
         :headers {"content-type" "text/clojure"}
         :body (with-out-str (clojure.pprint/pprint request))}
        )
      )
    )

  (defonce server (jetty/run-jetty #'app {:port 8080 :join? false}))

  (.start server)
  ;;(.stop server)

  )

(defn add-to-inbox
  "Add song to Inbox-playlist, create it if not existing"
  [spotify-uri]
  nil ;; TODO
  )
