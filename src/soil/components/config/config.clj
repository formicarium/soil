(ns soil.components.config.config
  (:require [com.stuartsierra.component :as component]
            [soil.protocols.config.config :as p-cfg]
            [schema.core :as s]
            [clojure.java.io :as io]
            [beamly-core.config :as cfg]))

(defrecord Config []
  p-cfg/Config
  (get-config [this path] (get-in (:config this) path))
  component/Lifecycle
  (start [this]
    (assoc this :config (cfg/load-config (.getPath (io/resource (str (name (:env this)) ".conf"))))))
  (stop [this] (dissoc this :config)))

(defn new-config [env]
  (map->Config {:env env}))
