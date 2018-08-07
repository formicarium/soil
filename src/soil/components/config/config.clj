(ns soil.components.config.config
  (:require [com.stuartsierra.component :as component]
            [soil.protocols.config.config :as p-cfg]
            [aero.core :as aero]
            [clojure.java.io :as io]))

(defrecord Config []
  p-cfg/Config
  (get-config [this path] (get-in (:config this) path))
  component/Lifecycle
  (start [this]
    (assoc this :config (slurp (io/resource (str (name (:env this)) ".edn")))))
  (stop [this]
    (dissoc this :config)))

(defn new-config [env]
  (map->Config {:env env}))
