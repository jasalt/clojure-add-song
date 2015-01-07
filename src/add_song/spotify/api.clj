(ns add-song.spotify.api
  "Spotify API authentication and operations.
  Spotify authorization code login as in
  https://developer.spotify.com/web-api/authorization-guide/"
  (:require
   [clj-http.client        :as client]
   [clojure.data.json      :as json]
   [clojure.java.io        :as io]
   [environ.core           :refer [env]]
   [ring.adapter.jetty     :as jetty]
   [ring.middleware.params :as ring-params]
   [clj-time.local         :as l]
   [clojure.java.browse    :as browser]
   [base64-clj.core        :as base64]
   [clojure.pprint         :refer [pprint]]
   ))

;; Load environment variables from ~/.lein/profiles.clj with lein-environ
;; leiningen plugin
(def spotify-client-id (env :spotify-client-id))
(def spotify-client-secret (env :spotify-client-secret))

(def callback-uri "http://localhost:8080/callback")

(def token-cache-dir (str (env :home) "/.add-song/"))
(def token-cache-file (str (env :home) "/.add-song/tokens"))


(import [java.net URLEncoder])
(defn encode-params [request-params]
  "Encode map to query string parameters"
  (let [encode #(URLEncoder/encode (str %) "UTF-8")
        coded (for [[n v] request-params] (str (encode n) "=" (encodev)))]
    (apply str (interpose "&" coded))))

(defn serve-callback-page
  "Serve callback page to which Spotify login redirects.
  Return received query params."
  []
  (def result-promise (promise))

  (defn app [request]
    ;; Loggin utility
    (def server-log-file (clojure.java.io/writer "server.log" :append true))
    (defn log-server
      [what]
      (binding [*out* server-log-file]
        (println "" )
        (println "--log-entry--" (str (l/local-now)))
        (pprint what)))

    (log-server request)

    (if (= (request :uri) "/callback")
      (do (log-server "CALLBACK!")
          ;; Continue with callback response
          (deliver result-promise request)
          (future (Thread/sleep 3000) (.stop server))
          {:status 200
           :headers {"Content-Type" "text/html"}
           :body (-> "callback-page.html" io/resource slurp)});
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body "Nothing here... Go to /callback instead."}))

  (defonce server (jetty/run-jetty #'app {:port 8080 :join? false}))
  (.start server)

  ;; Wait for servers answer from other thread
  @result-promise
  )

(defn auth-request
  "Request authorization code
  (1) Send user to authorization page (2) user logs in and gives access
  (3) user is redirected to callback and authorization-code is received"
  []
  (println "Please log in to Spotify and give permission.")
  (browser/browse-url (str "https://accounts.spotify.com/authorize?"
                           (encode-params
                            {"client_id" spotify-client-id
                             "response_type" "code"
                             "redirect_uri" callback-uri
                             "scope" (str "playlist-read-private "
                                          "playlist-modify-private "
                                          "playlist-modify-public")})))

  (println (str "Starting callback-server on " callback-uri))
  ;; Wait for user to be redirected to callback
  ;; Extract response code from query string
  (get ((ring-params/assoc-query-params (serve-callback-page) "UTF-8")
        :query-params) "code"))

(defn token-request
  "(4) Swap authorization-code for access and refresh tokens"
  [authorization-code]
  (let [auth-response (client/post "https://accounts.spotify.com/api/token"
                                   {:form-params
                                    {:grant_type "authorization_code"
                                     :code authorization-code
                                     :redirect_uri callback-uri}
                                    :headers
                                    {"Authorization"
                                     (str "Basic "
                                          (base64/encode
                                           (str spotify-client-id ":"
                                                spotify-client-secret)))}})]
    (println "Received auth token")
    (-> auth-response (:body) (json/read-str :key-fn keyword))))

(defn fetch-new-tokens
  "Fetch new tokens."
  []
  (let [auth-code (auth-request)
        auth-tokens (token-request auth-code)]

    (println "Saving received token")
    (pprint auth-tokens)

    (if (false? (.isDirectory (io/file token-cache-dir)))
      (.mkdir (io/file token-cache-dir)))

    (spit token-cache-file auth-tokens)
    ;; TODO save date
    )
  )

(defn refresh-tokens
  "(6) Use refresh token to get new access token"
  []
  nil ;;TODO
  )

(defn read-saved-tokens
  "Read saved access tokens from file"
  []
  (try
    (read-string (slurp token-cache-file))
    (catch Exception e (println "No saved tokens found") nil))
  )

(defn give-tokens
  "Hands a working Spotify access token, fetches new one if missing."
  []
  (let [old-tokens (read-saved-token)]
    (if old-tokens
      old-tokens ;; TODO Check if old-tokens have dried out
      (fetch-new-tokens)
      )
    )
  )


;; API operations
;; TODO separate namespaces

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
