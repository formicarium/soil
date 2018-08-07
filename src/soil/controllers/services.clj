(ns soil.controllers.services
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.protocols.configserver.configserver-client :as p-cs]
            [soil.protocols.config.config :as cfg]
            [soil.diplomat.kubernetes :as d-k8s]
            [soil.diplomat.hive :as hive]
            [soil.logic.services :as l-svc]
            [io.pedestal.log :as log]))

(defn create-kubernetes-resources!
  [{:keys [deployment ingress service tcp-services]} k8s-client]
  {:deployment   (get-in (p-k8s/create-deployment k8s-client deployment) [:metadata :name])
   :service      (get-in (p-k8s/create-service k8s-client service) [:metadata :name])
   :ingress      (get-in (p-k8s/create-ingress k8s-client ingress) [:metadata :name])
   :tcp-services (:data (d-k8s/add-tcp-ports tcp-services nil k8s-client))})

(defn to-kubernetes-resources [devspace config svc-config]
  (l-svc/config->kubernetes svc-config devspace (cfg/get-config config [:formicarium :domain])))

(defn notify-hive
  [devspace service-name]
  (try
    (hive/notify-service-deployed devspace service-name)
    (catch Exception e
      (log/error :exception e)
      nil)))

(defn deploy-service!
  [service-args devspace k8s-client config-server config]
  (let [resources (create-kubernetes-resources! (->> service-args
                                                     (p-cs/on-deploy-service config-server)
                                                     (to-kubernetes-resources devspace config))
                                                k8s-client)]
    resources))

(defn destroy-service!
  [service-name devspace k8s-client]
  {:deployment   (do (p-k8s/delete-deployment k8s-client service-name devspace) "deleted")
   :service      (do (p-k8s/delete-service k8s-client service-name devspace) "deleted")
   :ingress      (do (p-k8s/delete-ingress k8s-client service-name devspace) "deleted")
   :tcp-services (do (d-k8s/delete-tcp-ports [service-name] k8s-client) "deleted")})

#_(defn create-hive!
    [devspace k8s-client]
    (p-k8s/create-deployment k8s-client))
