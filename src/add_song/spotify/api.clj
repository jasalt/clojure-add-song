(ns add-song.spotify.api
  "Spotify API stuff"
  (:require
   [clj-http.client        :as client]
   [clojure.data.json      :as json]
   [environ.core           :refer [env]]
   [ring.adapter.jetty     :as jetty]
   [clj-time.local         :as l]
   [clojure.java.browse    :as browser]
   clojure.pprint
   ))



;; Load environment variables from ~/.lein/profiles.clj with lein-environ
;; leiningen plugin
(def spotify-client-id (env :spotify-client-id))
(def spotify-client-secret (env :spotify-client-secret))

(import [java.net URLEncoder])
(defn encode-params [request-params]

  (let [encode #(URLEncoder/encode (str %) "UTF-8")
        coded (for [[n v] request-params] (str (encode n) "=" (encode
                                                               v)))]
    (apply str (interpose "&" coded))))

(defn search-tracks
  "Search track from public Spotify web api"
  [artist title]
  (-> (client/get
       (str "http://ws.spotify.com/search/1/track.json?q="
            (str artist " " title)))
      (:body) (json/read-str :key-fn keyword)) )


(defn serve-callback-page
  "Show permission page for user"
  []

  (def result-promise (promise))
  (defonce server (jetty/run-jetty #'app {:port 8080 :join? false}))
  (defn app [request]

    ;; Loggin utility
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
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body "Nothing here... Go to /callback instead."}))

  (.start server)

  ;; Wait for servers answer from other thread
  @result-promise
  )

(defn auth-request
  "TODO First authorization step described in
  https://developer.spotify.com/web-api/authorization-guide/"
  []
  ;; TODO OAuth dance, store tokens, auto refresh expiring tokens

  ;; Send user to do auth
  (browser/browse-url (str "https://accounts.spotify.com/authorize?"
                           (encode-params
                            {"client_id" spotify-client-id
                             "response_type" "code"
                             "redirect_uri" "http://localhost:8080/callback"
                             "scope" (str "playlist-read-private "
                                          "playlist-modify-private "
                                          "playlist-modify-public")})))

  
  (println "Starting callback-server on localhost")

  (serve-callback-page)
  ;; TODO Get token from callback
  )

(defn add-to-inbox
  "Add song to Inbox-playlist, create it if not existing"
  [spotify-uri]
  nil ;; TODO
  )
