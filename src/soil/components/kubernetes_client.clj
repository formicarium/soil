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
            [clojure.string :as str]
            [clj-service.misc :as misc]))

(def KubernetesContext {s/Keyword s/Any})
(def debugctx (k8s/make-context "http://localhost:9000"))

(defn- keyword-keys->str-keys [m] (misc/map-keys #(subs (str %) 1) m))

(defn internalize-deployment
  [deployment]
  (-> deployment
      (update-in [:metadata :labels] keyword-keys->str-keys)
      (update-in [:metadata :annotations] keyword-keys->str-keys)
      (update-in [:spec :selector :matchLabels] keyword-keys->str-keys)
      (update-in [:spec :template :annotations] keyword-keys->str-keys)
      (update-in [:spec :template :labels] keyword-keys->str-keys)))

(defn internalize-service
  [service]
  (-> service
      (update-in [:metadata :labels] keyword-keys->str-keys)
      (update-in [:metadata :annotations] keyword-keys->str-keys)
      (update-in [:spec :selector] keyword-keys->str-keys)))

(defn internalize-ingress [ingress]
  (-> ingress
      (update-in [:metadata :labels] keyword-keys->str-keys)
      (update-in [:metadata :annotations] keyword-keys->str-keys)))

(defn internalize-pod [pod]
  (-> pod
      (update-in [:metadata :labels] keyword-keys->str-keys)))


(defn testing-logs [ctx pod-name namespace-name]
  @(http/get (str (:server ctx) (str "/api/v1/namespaces/" namespace-name "/pods/" pod-name "/log"))
             {:headers   {"Authorization" (str "Bearer " (:token ctx))}
              :as        :stream
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
  (<!! (k8s/create-namespace ctx k8s-namespace)))

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

(s/defn delete-all-ingresses!
  [ctx :- KubernetesContext
   namespace-name :- s/Str]
  (<!! (extensions-v1beta1/deletecollection-namespaced-ingress ctx {:namespace namespace-name})))

(s/defn list-ingresses
  [ctx :- KubernetesContext
   namespace-name :- s/Str
   opts :- (s/pred map?)]
  (mapv internalize-ingress (:items (<!! (extensions-v1beta1/list-namespaced-ingress ctx (assoc opts :namespace namespace-name))))))

(s/defn delete-service-account!
  [ctx :- KubernetesContext
   service-account-name :- s/Str
   namespace-name :- s/Str]
  (<!! (k8s/delete-namespaced-service-account ctx {} {:name service-account-name :namespace namespace-name})))

(s/defn list-deployment-impl :- [(s/pred map?)]
  [ctx :- KubernetesContext
   namespace :- s/Str
   opts :- (s/pred map?)]
  (let [deployments (:items (<!! (k8s-apps/list-namespaced-deployment ctx (merge opts {:namespace namespace}))))]
    (mapv internalize-deployment deployments)))

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
  (internalize-service (<!! (k8s/read-namespaced-service ctx {:namespace namespace
                                                              :name      service-name}))))

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
  (mapv internalize-pod (:items (<!! (k8s/list-namespaced-pod ctx (merge opts {:namespace namespace}))))))

(s/defn list-services-impl :- [(s/pred map?)]
  [ctx :- KubernetesContext
   namespace :- s/Str
   opts :- (s/pred map?)]
  (mapv internalize-service (:items (<!! (k8s/list-namespaced-service ctx (merge opts {:namespace namespace}))))))


(s/defn delete-all-services!
  [ctx :- KubernetesContext
   namespace-name :- s/Str]
  (mapv #(delete-service-impl! ctx (get-in % [:metadata :name]) namespace-name) (list-services-impl ctx namespace-name {})))

(defn check-api-health
  [ctx]
  (let [api-resources (<!! (k8s/get-api-resources ctx))]
    (not= (:success api-resources) false)))

(defn- throw-ex [name type code details]
  (log/error :log :exception-error :type type :name name :code code :details details)
  (throw (ex-info name (merge {:type type :code code :message name} details) (:exception details))))

(defn- conflict!
  [log-object]
  (throw-ex "Conflict" :conflict 409 log-object))

(defn- invalid-input!
  [log-object]
  (throw-ex "InvalidInput" :invalid-input 422 log-object))

(defn raise-errors! [apiserver-response]
  (log/info :log apiserver-response)
  (if (or (and (= (:kind apiserver-response) "Status") (not= (:status apiserver-response) "Success"))
          (nil? apiserver-response))
    (case (:code apiserver-response)
      401 (exception/unauthorized! {:log apiserver-response})
      403 (exception/forbidden! {:log apiserver-response})
      404 (exception/not-found! {:log apiserver-response})
      409 (conflict! {:log apiserver-response})
      422 (invalid-input! {:log apiserver-response})
      (exception/server-error! {:log apiserver-response}))
    apiserver-response))

(defn get-ingress-impl [ctx namespace-name ingress-name]
  (internalize-ingress (<!! (extensions-v1beta1/read-namespaced-ingress ctx {:namespace namespace-name
                                                                             :name      ingress-name}))))

(defn get-deployment-impl [ctx namespace deployment-name]
  (internalize-deployment (<!! (k8s-apps/read-namespaced-deployment ctx {:namespace namespace
                                                                         :name      deployment-name}))))

(defrecord KubernetesClient [config ctx]
  protocols.kubernetes-client/KubernetesClient
  (list-nodes [_]
    (-> (list-nodes-impl ctx)
        (raise-errors!)))

  (create-namespace! [_ k8s-namespace]
    (-> (create-namespace-impl! ctx k8s-namespace)
        (raise-errors!)))
  (list-namespaces [_]
    (protocols.kubernetes-client/list-namespaces ctx {}))
  (list-namespaces [_ opts]
    (-> (list-namespaces-impl ctx opts)
        (raise-errors!)))
  (delete-namespace! [_ namespace-name]
    (-> (delete-namespace-impl! ctx namespace-name)
        (raise-errors!)))
  (finalize-namespace! [_ namespace-name]
    (-> (finalize-namespace-impl! ctx namespace-name)
        (raise-errors!)))

  (get-ingress [_ ingress-name namespace]
    (-> (get-ingress-impl ctx namespace ingress-name)
        (raise-errors!)))
  (create-ingress! [_ ingress]
    (-> (create-ingress-impl! ctx ingress)
        (raise-errors!)))
  (list-ingresses [_ namespace-name opts]
    (-> (list-ingresses ctx namespace-name opts)
        (raise-errors!)))
  (list-ingresses [this namespace-name]
    (protocols.kubernetes-client/list-ingresses this namespace-name {}))
  (delete-ingress! [_ ingress-name namespace]
    (-> (delete-ingress-impl! ctx ingress-name namespace)
        (raise-errors!)))
  (delete-all-ingresses! [_ namespace]
    (-> (delete-all-ingresses! ctx namespace)
        (raise-errors!)))

  (get-service [_ service-name namespace]
    (-> (get-service-impl! ctx service-name namespace)
        (raise-errors!)))
  (list-services [_ namespace-name opts]
    (-> (list-services-impl ctx namespace-name opts)
        (raise-errors!)))
  (list-services [this namespace-name]
    (protocols.kubernetes-client/list-services this namespace-name {}))
  (create-service! [_ service]
    (-> (create-service-impl! ctx service)
        (raise-errors!)))
  (delete-service! [_ service-name namespace]
    (-> (delete-service-impl! ctx service-name namespace)
        (raise-errors!)))
  (delete-all-services! [_ namespace]
    (delete-all-services! ctx namespace))

  (get-deployment [_ deployment-name namespace]
    (-> (get-deployment-impl ctx namespace deployment-name)
        (raise-errors!)))
  (create-deployment! [_ deployment]
    (-> (create-deployment-impl! ctx deployment)
        (raise-errors!)))
  (list-deployment [_ namespace opts]
    (-> (list-deployment-impl ctx namespace opts)
        (raise-errors!)))
  (list-deployment [this namespace]
    (protocols.kubernetes-client/list-deployment this namespace {}))
  (delete-deployment! [_ deployment-name namespace]
    (-> (delete-deployment-impl! ctx deployment-name namespace)
        (raise-errors!)))
  (delete-all-deployments! [_ namespace]
    (-> (delete-all-deployments! ctx namespace)
        (raise-errors!)))

  (delete-service-account! [_ service-account-name namespace]
    (-> (delete-service-account! ctx service-account-name namespace)
        (raise-errors!)))

  (get-config-map [_ name namespace]
    (-> (get-config-map-impl ctx name namespace)
        (raise-errors!)))
  (patch-config-map! [_ name namespace config-map]
    (-> (patch-config-map-impl! ctx name namespace config-map)
        (raise-errors!)))

  (list-pods [_ namespace opts]
    (-> (list-pods-impl ctx namespace opts)
        (raise-errors!)))
  (list-pods [this namespace]
    (protocols.kubernetes-client/list-pods this namespace {}))

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
