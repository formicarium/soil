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
            [org.httpkit.client :as http]
            [clj-service.protocols.config :as protocols.config]
            [clojure.string :as str]))

(def KubernetesContext {s/Keyword s/Any})
(def ctx (k8s/make-context "http://localhost:9000"))

(defn testing-logs [ctx pod-name namespace-name]
  @(http/get (str (:server ctx) (str "/api/v1/namespaces/" namespace-name "/pods/" pod-name "/log"))
            {:headers {"Authorization" (str "Bearer " (:token ctx))}
             :as :stream
             :insecure? true}))

(defn- params->labels-query-string [m] (str/join "," (for [[k v] m] (str (name k) "=" v))))

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
  (<!! (k8s/patch-namespaced-config-map ctx config-map {:name      name
                                                        :namespace namespace})))

(s/defn create-namespace-impl!
  [ctx :- KubernetesContext
   k8s-namespace :- (s/pred map?)]
  (let [foo (<!! (k8s/create-namespace ctx k8s-namespace))]
    (prn ctx foo)
    foo))

(s/defn delete-namespace-impl!
  [ctx :- KubernetesContext namespace-name :- s/Str]
  (<!! (k8s/delete-namespace ctx {} {:name namespace-name})))

(s/defn finalize-namespace-impl!
  [ctx :- KubernetesContext
   namespace-name :- s/Str]
  (<!! (k8s/replace-namespace-finalize ctx {:metadata {:name namespace-name}} {:name namespace-name})))

(s/defn delete-all-deployments!
  [ctx :- KubernetesContext
   namespace-name :- s/Str]
  (<!! (k8s-apps/deletecollection-namespaced-deployment ctx {:namespace namespace-name} {})))

(s/defn list-all-services
  [ctx :- KubernetesContext
   namespace-name :- s/Str]
  (:items (<!! (k8s/list-namespaced-service ctx {:namespace namespace-name}))))

(s/defn delete-all-ingresses!
  [ctx :- KubernetesContext
   namespace-name :- s/Str]
  (<!! (extensions-v1beta1/deletecollection-namespaced-ingress ctx {:namespace namespace-name})))

(s/defn delete-service-account!
  [ctx :- KubernetesContext
   service-account-name :- s/Str
   namespace-name :- s/Str]
  (<!! (k8s/delete-namespaced-service-account ctx {} {:name service-account-name :namespace namespace-name})))

(s/defn list-deployment-impl :- [(s/pred map?)]
  [ctx :- KubernetesContext
   namespace :- s/Str
   opts :- (s/pred map?)]
  (<!! (k8s-apps/list-namespaced-deployment ctx (merge opts {:namespace namespace}))))

(s/defn delete-deployment-impl!
  [ctx :- KubernetesContext
   deployment-name :- s/Str
   deployment-namespace :- s/Str]
  (<!! (k8s-apps/delete-namespaced-deployment ctx {} {:name      deployment-name
                                                      :namespace deployment-namespace})))

(s/defn get-pod-container-logs
  [ctx :- KubernetesContext
   pod-name :- s/Str
   container-name :- s/Str
   namespace-name :- s/Str
   since-seconds :- (s/maybe s/Int)]
  (<!! (k8s/read-namespaced-pod-log ctx (into {} (filter (comp some? val)
                                                         {:name          pod-name
                                                          :namespace     namespace-name
                                                          :container     container-name
                                                          :since-seconds since-seconds})))))

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
  [ctx :- KubernetesContext
   opts :- (s/pred map?)]
  (:items (<!! (k8s/list-namespace ctx opts))))

(s/defn list-pods-impl :- [(s/pred map?)]
  [ctx :- KubernetesContext
   namespace :- s/Str
   opts :- (s/pred map?)]
  (:items (<!! (k8s/list-namespaced-pod ctx (merge opts {:namespace namespace})))))

(s/defn delete-all-services!
  [ctx :- KubernetesContext
   namespace-name :- s/Str]
  (mapv #(delete-service-impl! ctx (get-in % [:metadata :name]) namespace-name) (list-all-services ctx namespace-name)))

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
    (protocols.kubernetes-client/list-namespaces (:ctx this) {}))
  (list-namespaces [this opts]
    (-> (list-namespaces-impl (:ctx this) opts)
        (raise-errors!)))
  (delete-namespace! [this namespace-name]
    (-> (delete-namespace-impl! (:ctx this) namespace-name)
        (raise-errors!)))
  (finalize-namespace! [this namespace-name]
    (-> (finalize-namespace-impl! (:ctx this) namespace-name)
        (raise-errors!)))

  (create-ingress! [this ingress]
    (-> (create-ingress-impl! (:ctx this) ingress)
        (raise-errors!)))
  (delete-ingress! [this ingress-name namespace]
    (-> (delete-ingress-impl! (:ctx this) ingress-name namespace)
        (raise-errors!)))
  (delete-all-ingresses! [this namespace]
    (-> (delete-all-ingresses! (:ctx this) namespace)
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
  (delete-all-services! [this namespace]
    (delete-all-services! (:ctx this) namespace))

  (create-deployment! [this deployment]
    (-> (create-deployment-impl! (:ctx this) deployment)
        (raise-errors!)))
  (list-deployment [this namespace opts]
    (-> (list-deployment-impl (:ctx this) namespace opts)
        (raise-errors!)))
  (list-deployment [this namespace]
    (protocols.kubernetes-client/list-deployment this namespace {}))
  (delete-deployment! [this deployment-name namespace]
    (-> (delete-deployment-impl! (:ctx this) deployment-name namespace)
        (raise-errors!)))
  (delete-all-deployments! [this namespace]
    (-> (delete-all-deployments! (:ctx this) namespace)
        (raise-errors!)))

  (delete-service-account! [this service-account-name namespace]
    (-> (delete-service-account! (:ctx this) service-account-name namespace)
        (raise-errors!)))

  (get-config-map [this name namespace]
    (-> (get-config-map-impl (:ctx this) name namespace)
        (raise-errors!)))
  (patch-config-map! [this name namespace config-map]
    (-> (patch-config-map-impl! (:ctx this) name namespace config-map)
        (raise-errors!)))

  (list-pods [this namespace opts]
    (-> (list-pods-impl (:ctx this) namespace opts)
        (raise-errors!)))
  (list-pods [this namespace]
    (protocols.kubernetes-client/list-pods this namespace {}))

  component/Lifecycle
  (start [this]
    (let [kubernetes-url (protocols.config/get-in! config [:kubernetes :url])
          token-filepath (protocols.config/get-in! config [:kubernetes :token-filepath])

          ctx            (k8s/make-context kubernetes-url {:token (slurp token-filepath)})]
      (assoc this
        :ctx ctx
        :health (check-api-health ctx))))
  (stop [this] (dissoc this :ctx)))

(defn new-k8s-client
  [] (map->KubernetesClient {}))
