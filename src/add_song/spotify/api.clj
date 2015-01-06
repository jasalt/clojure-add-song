(ns add-song.spotify.api
  "Spotify API stuff"
  (:require
   [clj-http.client        :as client]
   [clojure.data.json      :as json]
   [environ.core           :refer [env]]
   [ring.adapter.jetty     :as jetty]
   [clj-time.local         :as l]
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
      (:body) (json/read-str :key-fn keyword)) )


(defn serve-permission-page
  "Show permission page for user"
  [auth-req-html]

  (def result-promise (promise))
  (defonce server (jetty/run-jetty #'app {:port 8080 :join? false}))
  (defn app [request]
    (def server-log-file (clojure.java.io/writer "server.log" :append true))
    (defn log-server
      [what]
      (binding [*out* server-log-file]
        (println "" )
        (println "--log-entry--" (str (l/local-now)))
        (clojure.pprint/pprint what)))

    (log-server request)
    (if (= (request :uri) "/callback")
      (do (log-server "CALLBACK!")
          ;; Continue with callback response
          (deliver result-promise request)
          (.stop server)
          )
      (do (log-server "Serve Spotify stuff")
          ;;TODO need to pass headers and other stuff?
          {:status 200
           :headers {"Content-Type" "text/html"}
           :body auth-req-html})))

  (.start server)

  (println "Started server")

  ;; Wait for servers answer from other thread
  @result-promise
  )

(defn auth-request
  "TODO First authorization step described in
  https://developer.spotify.com/web-api/authorization-guide/"
  []
  ;; OAuth dance, store tokens, auto refresh expiring tokens
  (let [authorization-request
        (client/get
         "https://accounts.spotify.com/authorize"
         {:query-params
          {:client_id spotify-client-id
           :response_type "code"
           :redirect_uri "http://localhost:8080/callback"
           :scope (str "playlist-read-private "
                       "playlist-modify-private "
                       "playlist-modify-public")}})]
    (println (authorization-request :body))
    (serve-permission-page (authorization-request :body))))



(defn add-to-inbox
  "Add song to Inbox-playlist, create it if not existing"
  [spotify-uri]
  nil ;; TODO
  )
