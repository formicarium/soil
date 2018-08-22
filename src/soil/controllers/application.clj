(ns soil.controllers.application
  (:require [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [schema.core :as s]
            [selmer.parser]
            [soil.models.application :as models.application]
            [soil.protocols.kubernetes-client :as protocols.k8s-client]
            [soil.protocols.etcd :as protocols.etcd]
            [soil.db.etcd.application :as etcd.application]
            [clj-service.protocols.config :as protocols.config]
            [soil.logic.application :as logic.application]))



(s/defn get-tcp-hosts :- {s/Str s/Str}
  [application :- models.application/Application
   k8s-client :- protocols.k8s-client/IKubernetesClient]
  (let [node-ip (diplomat.kubernetes/get-pod-node-ip application k8s-client)
        node-ports (diplomat.kubernetes/get-applications-node-ports application k8s-client)]
    (->> (logic.application/get-tcp-like-interfaces application)
         (mapv (fn [{:interface/keys [name]}]
                 [name (str node-ip ":" (get node-ports name))]))
         (into {}))))

(s/defn render-application :- models.application/Application
  [application :- models.application/Application
   k8s-client :- protocols.k8s-client/KubernetesClient]
  (->> (get-tcp-hosts application k8s-client)
       (logic.application/render-tcp-hosts application)))

(s/defn create-application! :- models.application/Application
  [application :- models.application/Application
   etcd :- protocols.etcd/IEtcd
   config :- protocols.config/IConfig
   k8s-client :- protocols.k8s-client/IKubernetesClient]
  (diplomat.kubernetes/create-deployment! application k8s-client)
  (diplomat.kubernetes/create-service! application k8s-client)
  (when (logic.application/has-http-like-interfaces application)
    (diplomat.kubernetes/create-ingress! application k8s-client))
  (when (logic.application/has-tcp-like-interfaces application)
    (diplomat.kubernetes/add-tcp-config-map-entries! application config k8s-client))
  (etcd.application/create-application! application etcd)
  (render-application application k8s-client))
