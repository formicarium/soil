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
            [soil.adapters.service :as adapters.service]
            [soil.db.etcd.application :as etcd.application]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]))

(s/defn create-service! :- models.application/Application
  [service-deploy :- schemas.service/DeployService,
   devspace :- s/Str
   config :- protocols.config/IConfig
   etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s-client/IKubernetesClient
   config-server :- protocols.config-server-client/IConfigServerClient]
  (-> (or (adapters.service/service-deploy+devspace->application? service-deploy devspace config)
          (diplomat.config-server/get-service-application devspace service-deploy config config-server))
      (logic.application/with-syncable-config (protocols.config/get! config :domain))
      (controllers.application/create-application! etcd config k8s-client)))

(s/defn ^:private try-delete :- s/Str
  [delete-fn :- (s/make-fn-schema s/Any [[protocols.k8s-client/KubernetesClient s/Str s/Str]])
   service-name :- s/Str
   devspace :- s/Str
   k8s-client :- protocols.k8s-client/KubernetesClient]
  (try (delete-fn k8s-client service-name devspace)
       "deleted"
       (catch Exception e
         (.printStackTrace e)
         "not-deleted")))

(s/defn delete-service!
  [service-name :- s/Str
   devspace :- s/Str
   etcd :- protocols.etcd/IEtcd
   config :- protocols.config/IConfig
   k8s-client :- protocols.k8s-client/KubernetesClient]
  (let [{application :value} (etcd.application/get-application! devspace service-name etcd)]
    (etcd.application/delete-application! service-name devspace etcd)
    {:deployment   (try-delete protocols.k8s-client/delete-deployment! service-name devspace k8s-client)
     :service      (try-delete protocols.k8s-client/delete-service! service-name devspace k8s-client)
     :ingress      (try-delete protocols.k8s-client/delete-ingress! service-name devspace k8s-client)
     :tcp-services (try (diplomat.kubernetes/delete-tcp-config-map-entries! application config k8s-client)
                        "deleted"
                        (catch Exception e
                          (.printStackTrace e)
                          "not-deleted"))}))

(s/defn one-service :- models.application/Application
  [devspace-name :- s/Str
   service-name :- s/Str
   etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s-client/KubernetesClient]
  (let [{application :value} (etcd.application/get-application! devspace-name service-name etcd)]
    (controllers.application/render-application application k8s-client)))
