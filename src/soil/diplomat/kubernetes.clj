(ns soil.diplomat.kubernetes
  (:require [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.adapters.devspace :as adapters.devspace]
            [clojure.set :refer [difference]]
            [schema.core :as s]
            [soil.logic.kubernetes :as logic.kubernetes]
            [soil.adapters.application :as adapters.application]
            [soil.models.application :as models.application]
            [clj-service.protocols.config :as protocols.config]
            [soil.logic.interface :as logic.interface]
            [soil.schemas.kubernetes.deployment :as schemas.k8s.deployment]
            [clj-service.misc :as misc]
            [soil.models.devspace :as models.devspace]
            [soil.logic.application :as logic.application]
            [io.pedestal.log :as log]
            [clj-service.adapt :as adapt]
            [soil.schemas.kubernetes.service :as schemas.k8s.service]
            [soil.schemas.kubernetes.ingress :as schemas.k8s.ingress]
            [clojure.string :as str]))

(s/defn create-namespace! :- s/Str
  [namespace-name :- s/Str
   args :- (s/pred map?)
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (adapters.devspace/devspace-name->create-namespace namespace-name args)
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

(s/defn get-pod-by-app-name :- (s/maybe (s/pred map?))
  [namespace-name :- s/Str
   app-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (protocols.k8s/list-pods k8s-client namespace-name)
       (filter #(= app-name (get-in % [:metadata :labels "formicarium.io/application"])))
       first))

(s/defn get-pod-by-app :- (s/maybe (s/pred map?))
  [{:application/keys [devspace name]} :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (get-pod-by-app-name devspace name k8s-client))

(s/defn get-pod-node-name :- (s/maybe s/Str)
  [application :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (get-in (get-pod-by-app application k8s-client) [:spec :nodeName]))

(s/defn get-pod-node-ip :- s/Str
  [application :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (protocols.k8s/list-nodes k8s-client)
       (filter (fn [node] (= (get-in node [:metadata :name])
                             (get-pod-node-name application k8s-client))))
       first
       logic.interface/get-node-ip))

(s/defn get-node-by-app-name :- (s/maybe (s/pred map?))
  [namespace-name :- s/Str
   app-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (protocols.k8s/list-nodes k8s-client)
       (filter (fn [node] (= (get-in node [:metadata :name])
                             (get-in (get-pod-by-app-name namespace-name app-name k8s-client) [:spec :nodeName]))))
       first))

(s/defn get-applications-node-ports :- {s/Str s/Int}
  [{:application/keys [name devspace]} :- models.application/Application
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (protocols.k8s/get-service k8s-client name devspace)
       :spec
       :ports
       (mapv (fn [{:keys [name nodePort]}] {name nodePort}))
       (apply merge)))

(s/defn ^:private get-fmc-namespaces :- [(s/pred map?)]
  [k8s-client :- protocols.k8s/IKubernetesClient]
  (protocols.k8s/list-namespaces k8s-client {:label-selector "formicarium.io/kind=fmc-devspace"}))

(s/defn get-devspaces-names :- [s/Str]
  [k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (get-fmc-namespaces k8s-client)
       (remove #(= :terminating (-> % :status :phase str/lower-case keyword)))
       (mapv (comp :name :metadata))))

(s/defn get-devspace-args :- (s/pred map?)
  [devspace-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (log/error :k8s-client k8s-client :devspace-name devspace-name)
  (adapt/from-edn
    (get-in
      (->> (get-fmc-namespaces k8s-client)
           (filter (fn [k8s-namespace] (= (get-in k8s-namespace [:metadata :name])
                                          devspace-name)))
           first)
      [:metadata :annotations "formicarium.io/args"])))

(s/defn get-deployments :- [schemas.k8s.deployment/Deployment]
  [namespace :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (protocols.k8s/list-deployment k8s-client namespace))

(s/defn app-name->k8s-label :- {:label-selector s/Str}
  [app-name :- s/Str]
  {:label-selector (str "formicarium.io/application=" app-name)})

(s/defn svc-name->k8s-label :- {:label-selector s/Str}
  [svc-name :- s/Str]
  {:label-selector (str "formicarium.io/service=" svc-name)})

(s/defn get-deployments-for-service :- [schemas.k8s.deployment/Deployment]
  [devspace-name :- s/Str
   fmc-service :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (protocols.k8s/list-deployment k8s-client devspace-name (svc-name->k8s-label fmc-service)))

(s/defn ^:private get-deployments :- [schemas.k8s.deployment/Deployment]
  [k8s-client :- protocols.k8s/IKubernetesClient
   namespace-name :- s/Str]
  (filter
    (fn [deployment]
      (string? (get-in deployment [:metadata :labels "formicarium.io/application"])))
    (protocols.k8s/list-deployment k8s-client namespace-name)))

(s/defn get-applications-for-deployments
  [devspace-name :- s/Str
   deployments :- [schemas.k8s.deployment/Deployment]
   k8s-client :- protocols.k8s/IKubernetesClient]
  (let [services    (protocols.k8s/list-services k8s-client devspace-name)
        ingresses   (protocols.k8s/list-ingresses k8s-client devspace-name)
        pods        (protocols.k8s/list-pods k8s-client devspace-name)
        nodes       (protocols.k8s/list-nodes k8s-client)]
    (map
      (fn [deployment]
        (let [app-name (logic.kubernetes/res->app-name deployment)
              service (logic.kubernetes/find-by-app-name app-name services)
              ingress (logic.kubernetes/find-by-app-name app-name ingresses)
              pod (logic.kubernetes/find-by-app-name app-name pods)
              node (misc/find-first nodes #(= (get-in % [:metadata :name]) (get-in pod [:spec :nodeName])))]
          (adapters.application/k8s->application deployment service ingress node)))
      deployments)))

(s/defn get-applications :- [models.application/Application]
  [devspace-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (let [deployments (get-deployments k8s-client devspace-name)]
    (get-applications-for-deployments devspace-name deployments k8s-client)))

(s/defn get-devspace :- models.devspace/Devspace
  [devspace-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (let [applications (get-applications devspace-name k8s-client)]
    #:devspace {:name devspace-name
                :hive (logic.application/get-hive applications)
                :tanajura (logic.application/get-tanajura applications)
                :applications (logic.application/but-hive-tanajura applications)}))
