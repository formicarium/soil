(ns soil.server
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [clj-service.system :as system]
            [soil.components :as soil]))

(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (prn "Running dev system map")
  (system/bootstrap! (soil/system-map :dev)))

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (prn "Running prod system map")
  (system/bootstrap! (soil/system-map :prod)))

