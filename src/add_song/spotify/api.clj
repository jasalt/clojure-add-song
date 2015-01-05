(ns add-song.spotify.api
  "Spotify API stuff"
  (:require
   [clj-http.client        :as client]
   [clojure.data.json      :as json]
   [environ.core           :refer [env]]
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

  ;;spotify-client-id
  ;;response_type
  ;;redirect_uri
  ;;state,scope
  )
