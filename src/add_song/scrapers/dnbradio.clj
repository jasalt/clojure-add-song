(ns add-song.scrapers.dnbradio
  "Scraper for DnB Radio currently playing song
  Using Ruby watir-webdriver.
  TODO Catch cookies and just curl."
  (:require
   [net.cgrand.enlive-html :as enlive]
   [clojure.string         :as string]
   [clojure.java.io        :as io]
   [environ.core           :refer [env]]
   [clj-commons-exec       :as exec]
   [clojure.pprint         :refer [pprint]]
   )
  )

;; Setup required env variables.
;; This is probably the thing that will break first.

(def good-path
  (let [missing-path
        (clojure.string/join ":"
                             (map
                              #(str (env :home) %)
                              ["/.rvm/gems/ruby-2.1.5/bin"
                               "/.rvm/gems/ruby-2.1.5@global/bin"
                               "/.rvm/rubies/ruby-2.1.5/bin"
                               "/.rvm/gems/ruby-2.1.5/bin"]))]
    (str missing-path ":" (env :path))))

(def damn-ruby
  {"DISPLAY" ":0.0"
   "GEM_HOME" (str (env :home) "/.rvm/gems/ruby-2.1.5")
   "GEM_PATH" (str (env :home) "/.rvm/gems/ruby-2.1.5:"
                   (env :home) "/.rvm/gems/ruby-2.1.5@global")
   "MY_RUBY_HOME" (str (env :home) "/home/js/.rvm/rubies/ruby-2.1.5")})

(def ruby-script-location (str (.getAbsolutePath (java.io.File. "")) "/src/add_song/scrapers/dnbradio.rb"))

(defn now-playing
  "Scrape now playing song from specified SomaFM station"
  [station]
  (let [process (exec/sh ["ruby" ruby-script-location ]
                         {:env (merge {"PATH" good-path} damn-ruby)})
        result (read-string (:out @process))]
    {:artist (result :artist)
     :title (clojure.string/replace (result :title) " [playlist rotation]" "")}))

;; :album (nth info-texts 3) ::TODO
;; :station-time

;; TODO station param not needed, refactor ...
;; TODO validation, check errors ...
;; TODO get the cookie and use it with their php api
