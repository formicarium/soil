(ns soil.components.kubernetes-api-server
  (:require [com.stuartsierra.component :as component]
            [me.raynes.conch.low-level :as sh]))

(defrecord KubernetesApiServer []
  component/Lifecycle
  (start [this]
    (assoc this :api-server (sh/proc "kubectl" "proxy" "--port=9000")))
  (stop [this]
    (.destroy (get-in this [:api-server :process]))
    (dissoc this :api-server)))

(defn new-k8s-api-server [] (->KubernetesApiServer))
