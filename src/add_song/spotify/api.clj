(ns add-song.spotify.api
  "Spotify API operations."
  (:require
   [clj-http.client        :as client]
   [clojure.data.json      :as json]

   [add-song.spotify.auth  :as auth]
   ))

;; API operations
;; TODO separate namespaces
;;

(def spotify-api-url "https://api.spotify.com/v1")

(defn parse-body-json
  [api-response]
  (-> api-response (:body) (json/read-str :key-fn keyword))
  )

(defn get-private
  "GET for private data"
  [api-endpoint]
  (client/get (str spotify-api-url api-endpoint)
              {:oauth-token (auth/get-access-token)
               :throw-entire-message? true}))

(defn get-user-id
  "Give current users Spotify user_id"
  [] (-> (get-private "/me") parse-body-json (:id)))

(defn list-user-playlists
  [user-id]
  (-> (get-private (str "/users/" user-id "/playlists"))
      parse-body-json))

(defn add-to-inbox
  "Add song to Inbox-playlist, create it if not existing"
  [spotify-uri]

  nil ;; TODO
  )

(defn search-tracks
  "Search track from public Spotify web api"
  [artist title]
  (-> (client/get
       (str "http://ws.spotify.com/search/1/track.json?q="
            (str artist " " title)))
      (:body) (json/read-str :key-fn keyword)))
