(ns soil.components.config.config
  (:require [com.stuartsierra.component :as component]
            [soil.protocols.config.config :as p-cfg]
            [schema.core :as s]
            [beamly-core.config :as cfg]))

(defrecord Config []
  p-cfg/Config
  (get-config [this path] (get-in (:config this) path))
  component/Lifecycle
  (start [this]
    (assoc this :config (cfg/load-config)))
  (stop [this] (dissoc this :config)))

(defn new-config []
  (map->Config {}))
