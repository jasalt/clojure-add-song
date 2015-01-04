(ns add-song.core
  "Raw clojure code."
  (:require
   [clojure.java.io        :as io]
   [clojure.string         :as string]
   [net.cgrand.enlive-html :as enlive]
   [clj-yaml.core          :as yaml]
   ))


(defn read-parse-yaml
  "Read and parse yaml-file from resources folder"
  [filename]
  (-> filename io/resource slurp yaml/parse-string)
  )


(defn scrape-somafm
  "Get currently playing song from Groove Salad"
  []
  (let [html (enlive/html-resource (io/as-url
                                    "http://somafm.com/groovesalad/songhistory.html"))
        song-elem (nth (enlive/select html [:tr]) 2)
        info-texts (map enlive/text
                        (enlive/select song-elem [:td]))]
    
    {:station-time (first (string/split (nth info-texts 0) #" "))
     :artist (nth info-texts 1)
     :title (nth info-texts 2)
     :album (nth info-texts 3)
     }))

(defn process-input
  [input-acronym station-list]
  (let [station (->> (map #(if (= input-acronym (% :acronym)) %)
                          (station-list :stations))
                     (filter identity) first)]
    (if station
      (do (println (scrape-somafm)) (println station) station)
      station
      )
    )
  )

(defn -main
  [& args]

  (if (and (= (count args) 1)
           (process-input (first args)
                           (read-parse-yaml "./radio-stations.yaml")))
    (println "Valid input! :))))")
    (println "Invalid input :(")
    )

  ;; TODO add help, generate station list...
  ;; usage: add-song station_acronym
  )
