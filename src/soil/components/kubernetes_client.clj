(ns soil.components.kubernetes-client
  (:require [clojure.core.async :refer [<!!]]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [schema.core :as s]
            [soil.protocols.kubernetes-client :as protocols.kubernetes-client]
            [soil.components.kubernetes.schema.deployment :as k8s.schema.deployment]
            [kubernetes.api.apps-v1 :as k8s-apps]
            [kubernetes.api.extensions-v1beta1 :as extensions-v1beta1]
            [kubernetes.api.v1 :as k8s]
            [clj-service.exception :as exception]
            [clj-service.protocols.config :as protocols.config]))

(def KubernetesContext {:server s/Str})
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
  (prn config-map)
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

(s/defn delete-deployment-impl!
  [ctx :- KubernetesContext
   deployment-name :- s/Str
   deployment-namespace :- s/Str]
  (<!! (k8s-apps/delete-namespaced-deployment ctx {} {:name      deployment-name
                                                      :namespace deployment-namespace})))

(s/defn create-deployment-impl!
  [ctx :- KubernetesContext
   deployment :- k8s.schema.deployment/Deployment]
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

(s/defn delete-service-impl!
  [ctx :- KubernetesContext
   name :- s/Str
   namespace :- s/Str]
  (<!! (k8s/delete-namespaced-service ctx {} {:name      name
                                              :namespace namespace})))

(defn list-namespaces-impl [ctx]
  (:items (<!! (k8s/list-namespace ctx))))

(defn check-api-health
  [ctx]
  (let [api-resources (<!! (k8s/get-api-resources ctx))]
    (not= (:success api-resources) false)))

(defn raise-errors! [apiserver-response]
  (log/info :log apiserver-response)
  (if (and (= (:kind apiserver-response) "Status") (not= (:status apiserver-response) "Success"))
    (exception/server-error! {:log apiserver-response})
    apiserver-response))

(defrecord KubernetesClient [config]
  protocols.kubernetes-client/KubernetesClient
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
  (delete-service! [this service-name namespace]
    (-> (delete-service-impl! (:ctx this) service-name namespace)
        (raise-errors!)))

  (create-deployment! [this deployment]
    (-> (create-deployment-impl! (:ctx this) deployment)
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

  component/Lifecycle
  (start [this]
    (let [ctx (k8s/make-context (protocols.config/get-in! config [:kubernetes :proxy :url]))]
      (assoc this
        :ctx ctx
        :health (check-api-health ctx))))
  (stop [this] (dissoc this :ctx)))

(defn new-k8s-client
  [] (map->KubernetesClient {}))
