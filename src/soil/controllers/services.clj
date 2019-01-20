(ns soil.controllers.services
  (:require [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.protocols.config-server-client :as protocols.config-server-client]
            [clj-service.protocols.config :as protocols.config]
            [soil.diplomat.config-server :as diplomat.config-server]
            [schema.core :as s]
            [soil.models.application :as models.application]
            [soil.logic.application :as logic.application]
            [soil.schemas.service :as schemas.service]
            [soil.controllers.application :as controllers.application]
            [soil.adapters.service :as adapters.service]
            [soil.adapters.application :as adapters.application]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [soil.protocols.kubernetes-client :as protocols.k8s]
            [clj-service.misc :as misc]))

(s/defn ^:private create-one-application :- models.application/Application
  [application :- models.application/Application
   config :- protocols.config/IConfig
   k8s-client :- protocols.k8s/IKubernetesClient]
  (-> (logic.application/with-syncable-config application (protocols.config/get! config :domain))
      (controllers.application/create-application! config k8s-client)))

(s/defn create-service! :- [models.application/Application]
  [service-deploy :- schemas.service/DeployService,
   devspace :- s/Str
   config :- protocols.config/IConfig
   k8s-client :- protocols.k8s/IKubernetesClient
   config-server :- protocols.config-server-client/IConfigServerClient]
  (->> (or (adapters.service/service-deploy+devspace->application? service-deploy devspace config)
           (diplomat.config-server/get-service-application devspace service-deploy config config-server))
       (mapv #(create-one-application % config k8s-client))))

(s/defn ^:private try-delete :- s/Str
  [delete-fn :- (s/make-fn-schema s/Any [[protocols.k8s/IKubernetesClient s/Str s/Str]])
   service-name :- s/Str
   devspace :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (try (delete-fn k8s-client service-name devspace)
       "deleted"
       (catch Exception e
         (.printStackTrace e)
         "not-deleted")))

(s/defn delete-service!
  [service-name :- s/Str
   devspace :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  {:deployment (try-delete protocols.k8s/delete-deployment! service-name devspace k8s-client)
   :service    (try-delete protocols.k8s/delete-service! service-name devspace k8s-client)
   :ingress    (try-delete protocols.k8s/delete-ingress! service-name devspace k8s-client)})

(s/defn one-service :- models.application/Application
  [devspace-name :- s/Str
   service-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (let [deployment (protocols.k8s/get-deployment k8s-client service-name devspace-name)
        service (protocols.k8s/get-service k8s-client service-name devspace-name)
        ingress (protocols.k8s/get-ingress k8s-client service-name devspace-name)
        node (diplomat.kubernetes/get-node-by-app-name devspace-name service-name k8s-client)]
    (prn "ONE SERVICE " (adapters.application/k8s->application deployment service ingress node))
    (adapters.application/k8s->application deployment service ingress node)))
