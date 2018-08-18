(ns soil.controllers.application
  (:require [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [schema.core :as s]
            [selmer.parser]
            [soil.models.application :as models.application]
            [soil.protocols.kubernetes-client :as protocols.k8s-client]
            [soil.protocols.etcd :as protocols.etcd]
            [soil.db.etcd.application :as etcd.application]))

(s/defn create-application! :- models.application/Application
  [application :- models.application/Application
   etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s-client/IKubernetesClient]
  (diplomat.kubernetes/create-deployment! application k8s-client)
  (diplomat.kubernetes/create-service! application k8s-client)
  (diplomat.kubernetes/create-ingress! application k8s-client)
  (etcd.application/create-application! application etcd)
  application)
