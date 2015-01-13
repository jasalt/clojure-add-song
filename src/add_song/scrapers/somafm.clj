(ns add-song.scrapers.somafm
  "Scraper for SomaFM channels currently playing song"
  (:require
   [net.cgrand.enlive-html :as enlive]
   [clojure.string         :as string]
   [clojure.java.io        :as io]
   )
  )

(defn now-playing
  "Scrape now playing song from specified SomaFM station"
  [station]
  (let [html (enlive/html-resource (io/as-url
                                    (station :song-history-url)))
        song-elem (nth (enlive/select html [:tr]) 2)
        info-texts (map enlive/text
                        (enlive/select song-elem [:td]))]

    {:station-time (first (string/split (nth info-texts 0) #" "))
     :artist (nth info-texts 1)
     :title (nth info-texts 2)
     :album (nth info-texts 3)
     })
  )
