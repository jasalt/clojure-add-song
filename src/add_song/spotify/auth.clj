(ns add-song.spotify.auth
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
   [clj-time.core          :as t]
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
        coded (for [[n v] request-params] (str (encode n) "=" (encode v)))]
    (apply str (interpose "&" coded))))

(defn serve-callback-page
  "Serve callback page to which Spotify login redirects.
  Return received query params."
  []
  (def result-promise (promise))
  
  (defonce server
    (jetty/run-jetty
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
          :body "Nothing here... Go to /callback instead."})) {:port 8080 :join? false}))

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
    (println "Received authorization code")
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

    (spit token-cache-file
          (merge auth-tokens {:date (str (l/local-now))}))))

(defn refresh-tokens
  "(6) Use refresh token to get new access token."
  [refresh-token]
  (let [refresh-response (client/post
                          "https://accounts.spotify.com/api/token"
                          {:form-params
                           {:grant_type "refresh_token"
                            :refresh_token refresh-token}
                           :headers
                           {"Authorization"
                            (str "Basic "
                                 (base64/encode
                                  (str spotify-client-id ":"
                                       spotify-client-secret)))}})
        fresh-tokens (-> refresh-response (:body)
                         (json/read-str :key-fn keyword) (merge {:refresh_token refresh-token}))]

    (spit token-cache-file
          (merge fresh-tokens
                 {:date (str (l/local-now))}))
    (println "Token refreshed.")
    fresh-tokens
    )
  )

(defn read-cached-tokens
  "Read saved access tokens from file"
  []
  (try
    (let [cached-tokens (read-string (slurp token-cache-file))
          date-cached (l/to-local-date-time (cached-tokens :date))]
      ;; Check that access token is fresh. Refresh if not.
      (if (> (cached-tokens :expires_in)
             (t/in-seconds (t/interval date-cached (l/local-now))))
        cached-tokens
        (refresh-tokens (cached-tokens :refresh_token))))

    (catch Exception e (println e) nil)))

(defn get-access-token
  "Hands a working Spotify access token, fetches new one if missing."
  []
  (let [cached-tokens (read-cached-tokens)]
    (if cached-tokens
      (cached-tokens :access_token)
      ((fetch-new-tokens) :access_token)
      )
    )
  )
