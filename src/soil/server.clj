(ns soil.server
  (:gen-class)
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as int]
            [com.stuartsierra.component :as component]
            [soil.component :as soil]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [soil.service :as service]))



(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (println "Running system map")
  (component/start-system (soil/system-map :dev)))

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")
  (component/start-system (soil/system-map :prod)))

