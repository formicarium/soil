(ns soil.components.kubernetes-client
  (:require [clojure.core.async :refer [<!!]]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [schema.core :as s]
            [soil.protocols.kubernetes-client :as protocols.kubernetes-client]
            [soil.schemas.kubernetes.deployment :as schemas.kubernetes.deployment]
            [kubernetes.api.apps-v1 :as k8s-apps]
            [kubernetes.api.extensions-v1beta1 :as extensions-v1beta1]
            [kubernetes.api.v1 :as k8s]
            [clj-service.exception :as exception]
            [clj-service.protocols.config :as protocols.config]))

(def KubernetesContext {s/Keyword s/Any})
(def ctx (k8s/make-context "http://localhost:9000"))

(s/defn get-config-map-impl
  [ctx :- KubernetesContext
   name :- s/Str
   namespace :- s/Str]
  (<!! (k8s/read-namespaced-config-map ctx {:name      name
                                            :namespace namespace})))

(s/defn patch-config-map-impl!
  [ctx :- KubernetesContext
   name :- s/Str
   namespace :- s/Str
   config-map]
  (<!! (k8s/patch-namespaced-config-map ctx
                                        config-map
                                        {:name      name
                                         :namespace namespace})))

(s/defn create-namespace-impl!
  [ctx :- KubernetesContext
   k8s-namespace :- (s/pred map?)]
  (<!! (k8s/create-namespace ctx k8s-namespace)))

(s/defn delete-namespace-impl!
  [ctx :- KubernetesContext namespace-name :- s/Str]
  (<!! (k8s/delete-namespace ctx {} {:name namespace-name})))

(s/defn list-deployment-impl :- [(s/pred map?)]
  [ctx :- KubernetesContext
   namespace :- s/Str]
  (<!! (k8s-apps/list-namespaced-deployment ctx {:namespace namespace})))

(s/defn delete-deployment-impl!
  [ctx :- KubernetesContext
   deployment-name :- s/Str
   deployment-namespace :- s/Str]
  (<!! (k8s-apps/delete-namespaced-deployment ctx {} {:name      deployment-name
                                                      :namespace deployment-namespace})))

(s/defn create-deployment-impl!
  [ctx :- KubernetesContext
   deployment :- schemas.kubernetes.deployment/Deployment]
  (<!! (k8s-apps/create-namespaced-deployment ctx deployment
                                              {:namespace (get-in deployment [:metadata :namespace])})))

(s/defn create-ingress-impl! [ctx ingress]
  (<!! (extensions-v1beta1/create-namespaced-ingress ctx ingress {:namespace (get-in ingress [:metadata :namespace])})))

(s/defn delete-ingress-impl!
  [ctx :- KubernetesContext
   ingress-name :- s/Str
   namespace :- s/Str]
  (<!! (extensions-v1beta1/delete-namespaced-ingress ctx {} {:name      ingress-name
                                                             :namespace namespace})))

(defn create-service-impl! [ctx service]
  (<!! (k8s/create-namespaced-service ctx service {:namespace (get-in service [:metadata :namespace])})))

(s/defn get-service-impl!
  [ctx service-name namespace]
  (<!! (k8s/read-namespaced-service ctx {:namespace namespace
                                         :name      service-name})))

(s/defn list-nodes-impl :- [(s/pred map?)]
  [ctx :- KubernetesContext]
  (:items (<!! (k8s/list-node ctx))))

(s/defn delete-service-impl!
  [ctx :- KubernetesContext
   name :- s/Str
   namespace :- s/Str]
  (<!! (k8s/delete-namespaced-service ctx {} {:name      name
                                              :namespace namespace})))

(s/defn list-namespaces-impl :- [(s/pred map?)]
  [ctx :- KubernetesContext]
  (:items (<!! (k8s/list-namespace ctx))))

(s/defn list-pods-impl :- [(s/pred map?)]
  [ctx :- KubernetesContext
   namespace :- s/Str]
  (:items (<!! (k8s/list-namespaced-pod ctx {:namespace namespace}))))

(defn check-api-health
  [ctx]
  (let [api-resources (<!! (k8s/get-api-resources ctx))]
    (not= (:success api-resources) false)))

(defn raise-errors! [apiserver-response]
  (log/info :log apiserver-response)
  (if (and (= (:kind apiserver-response) "Status") (not= (:status apiserver-response) "Success"))
    (case (:code apiserver-response)
      409 (throw (ex-info (:reason apiserver-response) (merge {:type type :code 409 :message (:reason apiserver-response)} apiserver-response) (:exception apiserver-response)))
      (exception/server-error! {:log apiserver-response}))
    apiserver-response))

(defrecord KubernetesClient [config]
  protocols.kubernetes-client/KubernetesClient
  (list-nodes [this]
    (-> (list-nodes-impl (:ctx this))
        (raise-errors!)))

  (create-namespace! [this k8s-namespace]
    (-> (create-namespace-impl! (:ctx this) k8s-namespace)
        (raise-errors!)))
  (list-namespaces [this]
    (-> (list-namespaces-impl (:ctx this))
        (raise-errors!)))
  (delete-namespace! [this namespace-name]
    (-> (delete-namespace-impl! (:ctx this) namespace-name)
        (raise-errors!)))

  (create-ingress! [this ingress]
    (-> (create-ingress-impl! (:ctx this) ingress)
        (raise-errors!)))
  (delete-ingress! [this ingress-name namespace]
    (-> (delete-ingress-impl! (:ctx this) ingress-name namespace)
        (raise-errors!)))

  (create-service! [this service]
    (-> (create-service-impl! (:ctx this) service)
        (raise-errors!)))
  (get-service [this service-name namespace]
    (-> (get-service-impl! (:ctx this) service-name namespace)
        (raise-errors!)))
  (delete-service! [this service-name namespace]
    (-> (delete-service-impl! (:ctx this) service-name namespace)
        (raise-errors!)))

  (create-deployment! [this deployment]
    (-> (create-deployment-impl! (:ctx this) deployment)
        (raise-errors!)))
  (list-deployment [this namespace]
    (-> (list-deployment-impl (:ctx this) namespace)
        (raise-errors!)))
  (delete-deployment! [this deployment-name namespace]
    (-> (delete-deployment-impl! (:ctx this) deployment-name namespace)
        (raise-errors!)))

  (get-config-map [this name namespace]
    (-> (get-config-map-impl (:ctx this) name namespace)
        (raise-errors!)))
  (patch-config-map! [this name namespace config-map]
    (-> (patch-config-map-impl! (:ctx this) name namespace config-map)
        (raise-errors!)))

  (list-pods [this namespace]
    (-> (list-pods-impl (:ctx this) namespace)
        (raise-errors!)))

  component/Lifecycle
  (start [this]
    (let [kubernetes-url (protocols.config/get-in! config [:kubernetes :url])
          token-filepath (protocols.config/get-in! config [:kubernetes :token-filepath])

          ctx (k8s/make-context kubernetes-url {:token (slurp token-filepath)})]
      (assoc this
        :ctx ctx
        :health (check-api-health ctx))))
  (stop [this] (dissoc this :ctx)))

(defn new-k8s-client
  [] (map->KubernetesClient {}))
