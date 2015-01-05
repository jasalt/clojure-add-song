(ns add-song.core
  "Load setup-file, parse command line arguments, do stuff."
  (:require
   ;; Import libraries and alias namespaces nicely with :as keyword
   [clojure.java.io        :as io]
   [clj-yaml.core          :as yaml]

   ;; Own modules/namespaces
   [add-song.spotify.api   :as spotify]
   [add-song.scrapers.somafm :as sfm]
   )(:gen-class))


(defn read-parse-yaml
  "Read and parse yaml-file from resources folder"
  [filename]
  (-> filename io/resource slurp yaml/parse-string)
  )

(defn scrape-station
  "Pass station by network to right scraper"
  [station]
  ;;TODO dnbradio.com scraper
  (let [station-network-scrapers
        {:SomaFM #(add-song.scrapers.somafm/now-playing %)}

        scraper (get station-network-scrapers
                     (keyword (station :network)))]

    (merge (scraper station) station)
    )
  )

(defn process-input
  "Check that input station acronym matches one in station list."
  [input-acronym station-list]
  (let [station (->> (map #(if (= input-acronym (% :acronym)) %)
                          (station-list :stations))
                     (filter identity) first)]
    (if station
      (scrape-station station)
      station
      )
    )
  )

(defn post-song
  "Temporary function that calls spotify-module"
  [artist title]
  (let [search-result (spotify/search-tracks artist title)]
    (println (first (search-result :tracks)))
    )
  ;; TODO post to user playlist if song found
  ;; TODO if not found post string to todo.txt (etc)
  )

(defn print-help
  []
  ;;TODO
  (println "Invalid input")
  (println "Usage: add-song station_acronym")
  )

(defn print-success
  [result]
  (println (str "Playing: " (result :artist) " - " (result :title)
                " @ " (result :network) " " (result :station)))
  )

(defn -main
  "Does simple validation for command argument and does stuff with it.
  TODO refactor."
  [& args]

  (if (= (count args) 1)
    (let [result
          (do (process-input (first args)
                             (read-parse-yaml "./radio-stations.yaml")))]
      (if result
        (do (post-song (result :artist) (result :title))
            (print-success result)
            )

        (print-help))
      )
    (print-help)
    )
  )
