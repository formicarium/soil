(ns soil.server
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [soil.components :as soil]))

(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (prn "Running system map")
  (component/start-system (soil/system-map :dev)))

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (prn "\nCreating your server...")
  (component/start-system (soil/system-map :dev)))

