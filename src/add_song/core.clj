(ns add-song.core
  "Raw clojure code."
  (:require
   [add-song.spotify.api   :as spotify]

   [clojure.java.io        :as io]

   [environ.core           :as env]

   [clj-yaml.core          :as yaml]

   [add-song.scrapers.somafm :as sfm]
   )(:gen-class))



;;(def spotify-client-id (env :spotify-client-id))
;;(def spotify-client-secret (env :spotify-client-secret))


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
  [& args]
  ;; TODO Allow doing search by input string instead of scraping stations
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
