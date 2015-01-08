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
  ([api-endpoint opt-params]
   (client/get (str spotify-api-url api-endpoint)
               (merge {:oauth-token (auth/get-access-token)
                       :throw-entire-message? true} opt-params)))
  ([api-endpoint]
   (get-private api-endpoint nil)
   )
  )

(defn post-private
  "POST for private data"
  [user-id api-endpoint form-params]
  (client/post (str spotify-api-url "/users/" user-id api-endpoint)
               {:oauth-token (auth/get-access-token)
                :throw-entire-message? true
                :content-type :json
                :accept :json
                :form-params form-params}
               )
  )

(defn get-user-id
  "Give current users Spotify user_id"
  [] (-> (get-private "/me") parse-body-json (:id)))

(defn list-user-playlists
  [user-id]
  (let [first-resp
        (-> (get-private (str "/users/" user-id "/playlists")
                         {:query-params {"limit" "50"
                                         "offset" "0"}})
            parse-body-json)
        api-limit (first-resp :limit)
        api-offset (first-resp :offset)
        api-total (first-resp :total)]

    (loop [iteration-offset (+ api-offset api-limit)
           playlists (first-resp :items)]
      (if (< api-total iteration-offset)
        (do (println "Got all")
            playlists)
        (do
          (println (str "iteration-offset "iteration-offset))
          (println (str "Getting " api-limit " from " iteration-offset " of " api-total))
          (recur (+ iteration-offset api-limit)
                 (concat playlists
                         ((-> (get-private
                               (str "/users/" user-id "/playlists")
                               {:query-params {"limit" api-limit "offset" iteration-offset}}) parse-body-json) :items))))))))

(defn create-playlist
  [user-id playlist-name]
  (post-private user-id "/playlists" {:name playlist-name})
  )

(defn playlist-exists
  "Search user playlist by name."
  [user-id name]
  (some #(when (= (% :name) name) %) (list-user-playlists user-id)))

(defn add-to-playlist
  [user-id playlist-id track-id]
  (post-private user-id "/playlists" {"uris" [track-id]})
  )

(defn add-to-inbox
  "Add song to Inbox-playlist, create it if not existing"
  [spotify-uri]

  (let [user-id (get-user-id)
        inbox-playlist (playlist-exists user-id inbox-playlist-name)]
    (when (nil? inbox-playlist)
      (pprint "Playlist not found. Creating..")
      (create-playlist user-id inbox-playlist-name)
      )
    (pprint "TODO add song there")
    )
  )

;;(add-to-inbox ((first ((search-tracks "netsky" "eyes") :tracks)):href))

(defn search-tracks
  "Search track from public Spotify web api"
  [artist title]
  (-> (client/get
       (str "http://ws.spotify.com/search/1/track.json?q="
            (str artist " " title)))
      (:body) (json/read-str :key-fn keyword)))
