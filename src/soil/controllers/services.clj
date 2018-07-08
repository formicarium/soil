(ns soil.controllers.services
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.protocols.configserver.configserver-client :as p-cs]
            [soil.logic.services :as l-svc]))

(defn deploy-service
  [service-args namespace k8s-client config-server]
  (println "deploy-service" namespace)
  (->> service-args
       (p-cs/on-deploy-service config-server)
       ((fn [svc-config] (l-svc/config->deployment svc-config namespace)))
       (p-k8s/create-deployment k8s-client)
       (l-svc/build-response)))

