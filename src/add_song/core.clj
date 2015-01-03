(ns add-song.core
  (:gen-class))

(require ['net.cgrand.enlive-html :as 'enlive])
(require ['clojure.java.io :as 'java])
(require ['clojure.string :as 'string])


(defn scrape-somafm
  "Get currently playing song from groove salad"
  []
  (let [html (enlive/html-resource (java/as-url
                                    "http://somafm.com/groovesalad/songhistory.html"))
        song-elem (nth (enlive/select html [:tr]) 2)
        info-texts (map enlive/text
                        (enlive/select song-elem [:td]))]

    {:time (first (string/split (nth info-texts 0) #" "))
     :artist (nth info-texts 1)
     :title (nth info-texts 2)
     :album (nth info-texts 3)
     }
    )
  )


(defn -main
  [& args]
  (println (scrape-somafm))
  )
