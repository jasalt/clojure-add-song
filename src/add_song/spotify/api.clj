(ns add-song.spotify.api
  "Spotify API operations."
  (:require
   [clj-http.client        :as client]
   [clojure.data.json      :as json]
   [clojure.pprint         :refer [pprint]]
   [add-song.spotify.auth  :as auth]
   ))

(def spotify-api-url "https://api.spotify.com/v1")

(def inbox-playlist-name "script-inbox")

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
  ;;TODO paginate through all
  (-> (get-private (str "/users/" user-id "/playlists"))
      parse-body-json))

(defn playlist-exists
  "Search user playlist by name."
  [name]
  (some #(when (= (% :name) name) %) ((list-user-playlists (get-user-id)) :items))
  )

;;(playlist-exists "Sleep")

(defn add-to-inbox
  "Add song to Inbox-playlist, create it if not existing"
  [spotify-uri]
  (let [inbox-playlist
        ((list-user-playlists (get-user-id))

         )])
  nil ;; TODO
  )

(defn search-tracks
  "Search track from public Spotify web api"
  [artist title]
  (-> (client/get
       (str "http://ws.spotify.com/search/1/track.json?q="
            (str artist " " title)))
      (:body) (json/read-str :key-fn keyword)))
