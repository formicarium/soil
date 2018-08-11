(ns soil.controllers.services
  (:require [soil.protocols.kubernetes-client :as protocols.kubernetes-client]
            [soil.protocols.config-server-client :as protocols.config-server-client]
            [clj-service.protocols.config :as protocols.config]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [soil.logic.services :as logic.service]
            [io.pedestal.log :as log]))

(defn create-kubernetes-resources!
  [{:keys [deployment ingress service tcp-services]} k8s-client]
  (log/info "create-kubernetes-resources!" service)
  {:deployment   (get-in (protocols.kubernetes-client/create-deployment! k8s-client deployment) [:metadata :name])
   :service      (mapv (fn [svc] (log/info "svc" svc) (get-in (protocols.kubernetes-client/create-service! k8s-client svc) [:metadata :name])) service)
   :ingress      (get-in (protocols.kubernetes-client/create-ingress! k8s-client ingress) [:metadata :name])
   :tcp-services (:data (diplomat.kubernetes/add-tcp-ports tcp-services nil k8s-client))})

(defn to-kubernetes-resources [devspace config svc-config]
  (logic.service/config->kubernetes svc-config devspace (protocols.config/get-in! config [:formicarium :domain])))

(defn deploy-service!
  [service-args devspace k8s-client config-server config]
  (let [resources (create-kubernetes-resources! (->> service-args
                                                     (protocols.config-server-client/on-deploy-service config-server)
                                                     (to-kubernetes-resources devspace config))
                    k8s-client)]
    resources))

(defn delete-ingresses [service-name devspace k8s-client]
  (protocols.kubernetes-client/delete-service! k8s-client (str service-name "-stinger") devspace)
  (protocols.kubernetes-client/delete-service! k8s-client (str service-name "-default") devspace)
  "deleted")

(defn destroy-service!
  [service-name devspace k8s-client]
  {:deployment   (do (protocols.kubernetes-client/delete-deployment! k8s-client service-name devspace) "deleted")
   :service      (delete-ingresses service-name devspace k8s-client)
   :ingress      (do (protocols.kubernetes-client/delete-ingress! k8s-client service-name devspace) "deleted")
   :tcp-services (do #_(diplomat.kubernetes/delete-tcp-ports [service-name] k8s-client) "deleted")})

#_(defn create-hive!
    [devspace k8s-client]
    (protocols.kubernetes-client/create-deployment! k8s-client))
