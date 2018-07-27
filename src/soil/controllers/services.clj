(ns soil.controllers.services
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.protocols.configserver.configserver-client :as p-cs]
            [soil.logic.services :as l-svc]))

(defn deploy-service!
  [service-args devspace k8s-client config-server]
  (let [{:keys [deployment ingress service]} (->> service-args
                                                  (p-cs/on-deploy-service config-server)
                                                  ((fn [svc-config] (l-svc/config->kubernetes svc-config devspace))))]
    (do
      (p-k8s/create-deployment k8s-client deployment)
      (p-k8s/create-service k8s-client service)
      (p-k8s/create-ingress k8s-client ingress))))

(defn destroy-service!
  [service-name devspace k8s-client]
  (p-k8s/delete-deployment k8s-client service-name devspace))

#_(defn create-hive!
  [devspace k8s-client]
  (p-k8s/create-deployment k8s-client))
