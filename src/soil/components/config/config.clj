(ns soil.components.config.config
  (:require [com.stuartsierra.component :as component]
            [soil.protocols.config.config :as p-cfg]
            [aero.core :as aero]
            [clojure.java.io :as io]))

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (if (some identity vs)
      (reduce #(rec-merge %1 %2) v vs)
      v)))

(defrecord Config []
  p-cfg/Config
  (get-config [this path] (get-in (:config this) path))
  component/Lifecycle
  (start [this]
    (assoc this :config (deep-merge
                          (aero/read-config (io/resource "base.edn"))
                          (aero/read-config (io/resource (str (name (:env this)) ".edn"))))))
  (stop [this]
    (dissoc this :config)))

(defn new-config [env]
  (map->Config {:env env}))
