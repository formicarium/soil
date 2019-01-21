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
   devspace-name :- s/Str
   config :- protocols.config/IConfig
   k8s-client :- protocols.k8s/IKubernetesClient
   config-server :- protocols.config-server-client/IConfigServerClient]
  (let [devspace-args (diplomat.kubernetes/get-devspace-args devspace-name k8s-client)]
    (->> (or (adapters.service/service-deploy+devspace->application? service-deploy devspace-name config)
             (diplomat.config-server/get-service-application devspace-name devspace-args service-deploy config config-server))
         (mapv #(create-one-application % config k8s-client)))))

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

(s/defn delete-application!
  [app-name :- s/Str
   devspace-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  {:name app-name
   :kubernetes
   {:deployment (try-delete protocols.k8s/delete-deployment! app-name devspace-name k8s-client)
    :service    (try-delete protocols.k8s/delete-service! app-name devspace-name k8s-client)
    :ingress    (try-delete protocols.k8s/delete-ingress! app-name devspace-name k8s-client)}})

(s/defn delete-service!
  [svc-name :- s/Str
   devspace-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (let [deployments (diplomat.kubernetes/get-deployments-for-service devspace-name svc-name k8s-client)
        apps (diplomat.kubernetes/get-applications-for-deployments devspace-name deployments k8s-client)]
    (mapv #(delete-application! (:application/name %) devspace-name k8s-client) apps)))

(s/defn one-service :- [models.application/Application]
  [devspace-name :- s/Str
   service-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (let [deployments (diplomat.kubernetes/get-deployments-for-service devspace-name service-name k8s-client)]
    (diplomat.kubernetes/get-applications-for-deployments devspace-name deployments k8s-client)))
