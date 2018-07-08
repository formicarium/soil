(ns soil.component
  (:require [com.stuartsierra.component :as component]
            [soil.service :as service]
            [soil.components.config.config :as config]
            [soil.components.api.soil-api :as soil-api]
            [soil.components.kubernetes.kubernetes-client :as kubernetes-client]
            [soil.components.configserver.configserver-client :as configserver-client]))

(defn system-map
  [env]
  {:config       (config/new-config)
   :service-map  (service/create-service env)
   :configserver (component/using (configserver-client/new-configserver) [:config])
   :k8s-client   (component/using (kubernetes-client/new-k8s-client) [:config])
   :soil-api     (component/using (soil-api/new-soil-api) [:k8s-client :service-map :configserver])})

