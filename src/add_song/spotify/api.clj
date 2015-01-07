(ns add-song.spotify.api
  "Spotify API authentication etc."
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

(import [java.net URLEncoder])
(defn encode-params [request-params]
  "Encode map to query string parameters"
  (let [encode #(URLEncoder/encode (str %) "UTF-8")
        coded (for [[n v] request-params] (str (encode n) "=" (encodev)))]
    (apply str (interpose "&" coded))))

(defn search-tracks
  "Search track from public Spotify web api"
  [artist title]
  (-> (client/get
       (str "http://ws.spotify.com/search/1/track.json?q="
            (str artist " " title)))
      (:body) (json/read-str :key-fn keyword)) )

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
  "First authorization step described in
  https://developer.spotify.com/web-api/authorization-guide/"
  []
  ;; Send user to do authentication in web browser
  (browser/browse-url (str "https://accounts.spotify.com/authorize?"
                           (encode-params
                            {"client_id" spotify-client-id
                             "response_type" "code"
                             "redirect_uri" callback-uri
                             "scope" (str "playlist-read-private "
                                          "playlist-modify-private "
                                          "playlist-modify-public")})))

  (println "Running callback-server on localhost.")
  (println "Please log in and agree to permissions.")

  ;; Extract response code from query string
  (get ((ring-params/assoc-query-params (serve-callback-page) "UTF-8")
        :query-params) "code"))

(defn token-request
  [authorization-code]
  ;;spotify-client-id
  (let [auth-response (client/post "https://accounts.spotify.com/api/token"
                                   {:form-params {:grant_type "authorization_code"
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

;;(def auth-tokens (token-request (auth-request)))
;;auth-tokens

(defn oauth-login
  []
  (let [auth-code (auth-request)
        auth-tokens (token-request auth-code)])
  )

(defn add-to-inbox
  "Add song to Inbox-playlist, create it if not existing"
  [spotify-uri]
  nil ;; TODO
  )
