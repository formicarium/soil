(ns soil.component
  (:require [com.stuartsierra.component :as component]
            [soil.service :as service]
            [soil.components.config.config :as config]
            [soil.components.api.soil-api :as soil-api]
            [soil.components.kubernetes.kubernetes-client :as kubernetes-client]
            [soil.components.kubernetes.kubernetes-api-server :as kubernetes-api-server]
            [soil.components.configserver.configserver-client :as configserver-client]))

(defn base [env]
  {:config         (config/new-config env)
   :service-map    (service/create-service env)
   :configserver   (component/using (configserver-client/new-configserver) [:config])
   :k8s-api-server (component/using (kubernetes-api-server/new-k8s-api-server) [:config])
   :k8s-client     (component/using (kubernetes-client/new-k8s-client) [:config :k8s-api-server])
   :soil-api       (component/using (soil-api/new-soil-api) [:k8s-client :service-map :configserver])})

(defn test-system []
  {:k8s-api-server {}})

(defn system-map
  [env]
  (case env
    "test" (merge (base env) (test-system))
    (base env)))

