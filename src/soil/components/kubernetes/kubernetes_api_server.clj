(ns soil.components.kubernetes.kubernetes-api-server
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [me.raynes.conch.low-level :as sh]))

(defn start-api-server!
  []
  (sh/proc "kubectl"
           "proxy"
           "--port=9000"))

(defrecord KubernetesApiServer [config]
  component/Lifecycle
  (start [this]
    (assoc this
      :api-server (start-api-server!)))
  (stop [this]
    (.destroy (get-in this [:api-server :process]))
    (dissoc this :api-server)))

(defn new-k8s-api-server [] (map->KubernetesApiServer {}))
