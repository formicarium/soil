(ns soil.server
  (:gen-class)
  (:require
    [clojure.tools.logging :as l]
    [com.stuartsierra.component :as component]
    [soil.component :as soil]))



(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (l/info "Running system map")
  (component/start-system (soil/system-map :dev)))

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (l/info "\nCreating your server...")
  (component/start-system (soil/system-map :prod)))

