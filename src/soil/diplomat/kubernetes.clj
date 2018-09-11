(ns soil.diplomat.kubernetes
  (:require [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.adapters.devspace :as adapters.devspace]
            [clojure.set :refer [difference]]
            [schema.core :as s]
            [soil.adapters.application :as adapters.application]
            [soil.models.application :as models.application]
            [clj-service.protocols.config :as protocols.config]
            [soil.logic.interface :as logic.interface]))

(s/defn create-namespace! :- s/Str
  [namespace-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (adapters.devspace/devspace-name->create-namespace namespace-name)
       (protocols.k8s/create-namespace! k8s-client))
  namespace-name)

(s/defn create-deployment!
  [application :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient
   config :- protocols.config/IConfig]
  (->> (adapters.application/application->deployment application (protocols.config/get-in-maybe config [:kubernetes :image-pull-secrets]))
       (protocols.k8s/create-deployment! k8s-client)))

(s/defn create-service!
  [application :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (adapters.application/application->service application)
       (protocols.k8s/create-service! k8s-client)))

(s/defn create-ingress!
  [application :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (adapters.application/application->ingress application)
       (protocols.k8s/create-ingress! k8s-client)))

(s/defn get-pod-by-app :- (s/maybe (s/pred map?))
  [{:application/keys [devspace name]} :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (protocols.k8s/list-pods k8s-client devspace)
       (filter #(= name (get-in % [:metadata :labels :app])))
       first))

(s/defn get-pod-node-name :- (s/maybe s/Str)
  [application :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (get-in (get-pod-by-app application k8s-client) [:spec :nodeName]))

(s/defn get-pod-node-ip :- {(s/maybe (s/pred map?)) (s/maybe (s/pred map?))}
  [application :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (protocols.k8s/list-nodes k8s-client)
       (filter (fn [node] (= (get-in node [:metadata :name]) (get-pod-node-name application k8s-client))))
       first
       logic.interface/get-node-ip))

(s/defn get-applications-node-ports :- {s/Str s/Int}
  [{:application/keys [name devspace]} :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (protocols.k8s/get-service k8s-client name devspace)
       :spec
       :ports
       (mapv (fn [{:keys [name nodePort]}] {name nodePort}))
       (apply merge)))
