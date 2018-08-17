(ns soil.controllers.services
  (:require [soil.protocols.kubernetes-client :as protocols.k8s-client]
            [soil.protocols.config-server-client :as protocols.config-server-client]
            [clj-service.protocols.config :as protocols.config]
            [soil.diplomat.config-server :as diplomat.config-server]
            [schema.core :as s]
            [soil.models.application :as models.application]
            [soil.logic.application :as logic.application]
            [soil.schemas.service :as schemas.service]
            [soil.controllers.application :as controllers.application]
            [soil.protocols.etcd :as protocols.etcd]
            [soil.db.etcd.application :as etcd.application]))

(s/defn create-service! :- models.application/Application
  [service-deploy :- schemas.service/DeployService,
   devspace :- s/Str
   config :- protocols.config/IConfig
   k8s-client :- protocols.k8s-client/IKubernetesClient
   config-server :- protocols.config-server-client/IConfigServerClient]
  (-> (diplomat.config-server/get-service-application devspace service-deploy config config-server)
      (logic.application/with-syncable-config (protocols.config/get-in! config [:formicarium :domain]))
      (controllers.application/create-application! k8s-client)))

(defn delete-ingresses [service-name devspace k8s-client]
  (protocols.k8s-client/delete-service! k8s-client (str service-name "-stinger") devspace)
  (protocols.k8s-client/delete-service! k8s-client (str service-name "-default") devspace)
  "deleted")

(defn destroy-service!
  [service-name devspace k8s-client]
  {:deployment   (do (protocols.k8s-client/delete-deployment! k8s-client service-name devspace) "deleted")
   :service      (do (protocols.k8s-client/delete-service! k8s-client service-name devspace) "deleted")
   :ingress      (do (protocols.k8s-client/delete-ingress! k8s-client service-name devspace) "deleted")
   :tcp-services (do #_(diplomat.kubernetes/delete-tcp-ports [service-name] k8s-client) "deleted")})

(s/defn one-service :- models.application/Application
  [devspace-name :- s/Str
   service-name :- s/Str
   etcd :- protocols.etcd/IEtcd]
  (:value (etcd.application/get-application! devspace-name service-name etcd)))
