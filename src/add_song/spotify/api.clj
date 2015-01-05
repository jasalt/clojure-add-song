(ns add-song.spotify.api
  (:require
   [clj-http.client        :as client]
   [clojure.data.json      :as json])
  )



;; Environment variables get loaded from ~/.lein/profiles.clj by lein-environ plugin

(def spotify-stuff "asdfasdfi!1")

(defn search-tracks
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
  ;;spotify-client-id
  ;;response_type
  ;;redirect_uri
  ;;state,scope
  )
