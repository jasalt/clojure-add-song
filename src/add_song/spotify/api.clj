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

(defn get-user-id
  "Give current users Spotify user_id"
  []
  (client/get "https://api.spotify.com/v1/me"
              {:headers
               {"Authorization"
                (str "Bearer "
                     (auth/get-access-token))}
               }))

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
