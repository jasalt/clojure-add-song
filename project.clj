;; Leiningen build tool project setup-file
(defproject add-song "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;; External libraries/dependecies are defined here and resolved beautifully by
  ;; leiningen by issuing `lein deps', `lein run' or `lein repl' command. 
  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.5"]
                 [enlive "1.1.5"]
                 [clj-yaml "0.4.0"]
                 [clj-http "1.0.1"]
                 [environ "1.0.0"]
                 [ring "1.3.2"]
                 [base64-clj "0.1.1"]
                 [org.clojars.hozumi/clj-commons-exec "1.1.0"]
                 ]
  :main ^:skip-aot add-song.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
