(ns soil.components.kubernetes-api-server
  (:require [com.stuartsierra.component :as component]
            [me.raynes.conch.low-level :as sh]
            [clj-service.protocols.config :as protocols.config]))

(defrecord KubernetesApiServer [config]
  component/Lifecycle
  (start [this]
    (if (protocols.config/get-maybe config :skip-kubectl-proxy)
      this
      (assoc this :api-server (sh/proc "kubectl" "proxy" "--port=9000"))))
  (stop [this]
    (.destroy (get-in this [:api-server :process]))
    (dissoc this :api-server)))

(defn new-k8s-api-server [] (map->KubernetesApiServer {}))
