(ns soil.components.kubernetes.kubernetes-client
  (:require [kubernetes.api.v1 :as k8s]
            [kubernetes.api.apps-v1 :as k8s-apps]
            [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [clojure.core.async :refer [<!!]]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [cheshire.core :as cheshire]
            [soil.protocols.config.config :as p-cfg]))

(s/defn create-namespace-impl [ctx namespace-name :- s/Str]
  (<!! (k8s/create-namespace ctx {:metadata {:name namespace-name}})))

(s/defn delete-namespace-impl [ctx namespace-name :- s/Str]
  (<!! (k8s/delete-namespace ctx {} {:name namespace-name})))

(s/defn create-deployment-impl [ctx deployment]
  (<!! (k8s-apps/create-namespaced-deployment ctx deployment
                                              {:namespace (get-in deployment [:metadata :namespace])})))

(defn list-namespaces-impl
  [ctx]
  (:items (<!! (k8s/list-namespace ctx))))

(defn delete-deployment-impl [ctx deployment-name deployment-namespace]
  (<!! (k8s-apps/delete-namespaced-deployment ctx {} {:name      deployment-name
                                                      :namespace deployment-namespace})))
(defn check-api-health
  [ctx]
  (let [api-resources (<!! (k8s/get-api-resources ctx))]
    (not= (:success api-resources) false)))

(defn raise-errors [apiserver-response]
  (println apiserver-response)
  (if (and (= (:kind apiserver-response) "Status")
           (not= (:status apiserver-response) "Success"))
    (do (println apiserver-response) (throw (ex-info "Error from ApiServer" apiserver-response)))
    apiserver-response))

(defrecord KubernetesClient [config]
  p-k8s/KubernetesClient
  (create-namespace [this namespace]
    (-> (create-namespace-impl (:ctx this) namespace)
        (raise-errors)))
  (delete-namespace [this namespace-name]
    (-> (delete-namespace-impl (:ctx this) namespace-name)
        (raise-errors)))
  (list-namespaces [this]
    (-> (list-namespaces-impl (:ctx this))
        (raise-errors)))
  (create-deployment [this deployment]
    (-> (create-deployment-impl (:ctx this) deployment)
        (raise-errors)))
  (delete-deployment [this deployment-name deployment-namespace]
    (-> (delete-deployment-impl (:ctx this) deployment-name deployment-namespace)
        (raise-errors)))

  component/Lifecycle
  (start [this]
    (let [ctx (k8s/make-context (p-cfg/get-config config [:kubernetes :proxy :url]))]
      (println ctx (check-api-health ctx))
      (assoc this
             :ctx ctx
             :health (check-api-health ctx))))
  (stop [this] (dissoc this :ctx)))

(defn new-k8s-client
  [] (map->KubernetesClient {}))

(def deploy-example
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:name      "redis-master"
                :namespace "test"}
   :spec       {:selector {:matchLabels {:app "redis"}}
                :replicas 1
                :template {:metadata {:labels {:app "redis"}}
                           :spec     {:containers [{:name      "master"
                                                    :image     "redis"
                                                    :resources {:requests {:cpu    "100m"
                                                                           :memory "100Mi"}}
                                                    :ports     [{:containerPort 6379}]}]}}}})
;; create-namespace
;; create-namespaced-binding
;; create-namespaced-config-map
;; create-namespaced-endpoints
;; create-namespaced-event
;; create-namespaced-limit-range
;; create-namespaced-persistent-volume-claim
;; create-namespaced-pod
;; create-namespaced-pod-binding
;; create-names
;; paced-pod-eviction
;; create-namespaced-pod-template
;; create-namespaced-replication-controller
;; create-namespaced-resource-quota
;; create-namespaced-secret
;; create-namespaced-service
;; create-namespaced-service-account
;; create-node
;; create-persistent-volume
;; delete-namespace
;; delete-namespaced-config-map
;; delete-namespaced-endpoints
;; delete-namespaced-event
;; delete-namespaced-limit-range
;; delete-namespaced-persistent-volume-claim
;; delete-namespaced-pod
;; delete-namespaced-pod-template
;; delete-namespaced-replication-controller
;; delete-namespaced-resource-quota
;; delete-namespaced-secret
;; delete-namespaced-service
;; delete-namespaced-service-account
;; delete-node
;; delete-persistent-volume
;; deletecollection-namespaced-config-map
;; deletecollection-namespaced-endpoints
;; deletecollection-namespaced-event
;; deletecollection-namespaced-limit-range
;; deletecollection-namespaced-persistent-volume-claim
;; deletecollection-namespaced-pod
;; deletecollection-namespaced-pod-template
;; deletecollection-namespaced-replication-controller
;; deletecollection-namespaced-resource-quota
;; deletecollection-namespaced-secret
;; deletecollection-namespaced-service-account
;; deletecollection-node
;; deletecollection-persistent-volume
;; get-api-resources
;; list-component-status
;; list-config-map-for-all-namespaces
;; list-endpoints-for-all-namespaces
;; list-event-for-all-namespaces
;; list-limit-range-for-all-namespaces
;; list-namespace
;; list-namespaced-config-map
;; list-namespaced-endpoints
;; list-namespaced-event
;; list-namespaced-limit-range
;; list-namespaced-persistent-volume-claim
;; list-namespaced-pod
;; list-namespaced-pod-template
;; list-namespaced-replication-controller
;; list-namespaced-resource-quota
;; list-namespaced-secret
;; list-namespaced-service
;; list-namespaced-service-account
;; list-node
;; list-persistent-volume
;; list-persistent-volume-claim-for-all-namespaces
;; list-pod-for-all-namespaces
;; list-pod-template-for-all-namespaces
;; list-replication-controller-for-all-namespaces
;; list-resource-quota-for-all-namespaces
;; list-secret-for-all-namespaces
;; list-service-account-for-all-namespaces
;; list-service-for-all-namespaces
;; make-context
;; patch-namespace
;; patch-namespace-status
;; patch-namespaced-config-map
;; patch-namespaced-endpoints
;; patch-namespaced-event
;; patch-namespaced-limit-range
;; patch-namespaced-persistent-volume-claim
;; patch-namespaced-persistent-volume-claim-status
;; patch-namespaced-pod
;; patch-namespaced-pod-status
;; patch-namespaced-pod-template
;; patch-namespaced-replication-controller
;; patch-namespaced-replication-controller-scale
;; patch-namespaced-replication-controller-status
;; patch-namespaced-resource-quota
;; patch-namespaced-resource-quota-status
;; patch-namespaced-secret
;; patch-namespaced-service
;; patch-namespaced-service-account
;; patch-namespaced-service-status
;; patch-node
;; patch-node-status
;; patch-persistent-volume
;; patch-persistent-volume-status
;; read-component-status
;; read-namespace
;; read-namespace-status
;; read-namespaced-config-map
;; read-namespaced-endpoints
;; read-namespaced-event
;; read-namespaced-limit-range
;; read-namespaced-persistent-volume-claim
;; read-namespaced-persistent-volume-claim-status
;; read-namespaced-pod
;; read-namespaced-pod-log
;; read-namespaced-pod-status
;; read-namespaced-pod-template
;; read-namespaced-replication-controller
;; read-namespaced-replication-controller-scale
;; read-namespaced-replication-controller-status
;; read-namespaced-resource-quota
;; read-namespaced-resource-quota-status
;; read-namespaced-secret
;; read-namespaced-service
;; read-namespaced-service-account
;; read-namespaced-service-status
;; read-node
;; read-node-status
;; read-persistent-volume
;; read-persistent-volume-status
;; replace-namespace
;; replace-namespace-finalize
;; replace-namespace-status
;; replace-namespaced-config-map
;; replace-namespaced-endpoints
;; replace-namespaced-event
;; replace-namespaced-limit-range
;; replace-namespaced-persistent-volume-claim
;; replace-namespaced-persistent-volume-claim-status
;; replace-namespaced-pod
;; replace-namespaced-pod-status
;; replace-namespaced-pod-template
;; replace-namespaced-replication-controller
;; replace-namespaced-replication-controller-scale
;; replace-namespaced-replication-controller-status
;; replace-namespaced-resource-quota
;; replace-namespaced-resource-quota-status
;; replace-namespaced-secret
;; replace-namespaced-service
;; replace-namespaced-service-account
;; replace-namespaced-service-status
;; replace-node
;; replace-node-status
;; replace-persistent-volume
;; replace-persistent-volume-status
#_(comment
    (def dontcare s/Any)
    (def meta.v1.ObjectMeta s/Any)
    (def meta.v1.LabelSelectorRequirement s/Any)
    (def meta.v1.LabelSelector s/Any)
    (def apps.v1.DeploymentStrategy s/Any)
    (def core.v1.ConfigMapKeySelector
      {:key      s/Str
       :name     (s/maybe s/Str)
       :optional (s/maybe s/Boolean)})
    (def core.v1.ObjectFieldSelector
      {:apiVersion (s/maybe s/Str)
       :fieldPath  s/Str})
    (def core.v1.ResourceFieldSelector
      {:containerName (s/maybe s/Str)
       :divisor       dontcare
       :resource      s/Str})
    (def core.v1.SecretKeySelector
      {:key      s/Str
       :name     (s/maybe s/Str)
       :optional (s/maybe s/Boolean)})
    (def core.v1.EnvFromSource
      {:configMapKeyRef  (s/maybe core.v1.ConfigMapKeySelector)
       :fieldRef         (s/maybe core.v1.ObjectFieldSelector)
       :resourceFieldRef (s/maybe core.v1.ResourceFieldSelector)
       :secretKeyRef     (s/maybe core.v1.SecretKeySelector)})
    (def core.v1.EnvVar
      {:name      s/Str
       :value     (s/maybe s/Str)
       :valueFrom (s/maybe core.v1.EnvVarSource)})
    (def core.v1.VolumeMount
      {:mountPath        s/Str
       :mountPropagation (s/maybe s/Str)
       :name             s/Str
       :readOnly         (s/maybe s/Boolean)
       :subPath          (s/maybe s/Str)})
    (def core.v1.VolumeDevice
      {:devicePath s/Str
       :name       s/Str})
    (def core.v1.ContainerPort
      {:containerPort s/Int
       :hostIP        (s/maybe s/Str)
       :hostPort      (s/maybe s/Int)
       :name          (s/maybe s/Str)
       :protocol      (s/enum "TCP" "UDP")})
    (def core.v1.Container
      {:args                     (s/maybe [s/Str])
       :command                  (s/maybe [s/Str])
       :env                      (s/maybe [core.v1.EnvVar])
       :envFrom                  (s/maybe [core.v1.EnvFromSource])
       :image                    (s/maybe s/Str)
       :imagePullPolicy          (s/maybe s/Str)
       :lifecycle                dontcare
       :livenessProbe            dontcare
       :name                     s/Str
       :ports                    (s/maybe [core.v1.ContainerPort])
       :readinessProbe           dontcare
       :resources                dontcare
       :securityContext          dontcare
       :stdin                    dontcare
       :stdinOnce                dontcare
       :terminationMessagePath   dontcare
       :terminationMessagePolicy dontcare
       :tty                      dontcare
       :volumeDevices            (s/maybe [core.v1.VolumeDevice])
       :volumeMounts             (s/maybe [core.v1.VolumeMount])
       :workingDir               s/Str})
    (def core.v1.PodSpec
      {:containers core.v1.Container})
    (def core.v1.PodTemplateSpec
      {:metadata meta.v1.ObjectMeta
       :spec     core.v1.PodSpec})
    (def apps.v1.DeploymentSpec
      {:minReadySeconds         (s/maybe s/Int)
       :paused                  (s/maybe s/Boolean)
       :progressDeadlineSeconds (s/maybe s/Boolean)
       :replicas                (s/maybe s/Int)
       :revisionHistoryLimit    (s/maybe s/Int)
       :selector                meta.v1.LabelSelector
       :strategy                (s/maybe apps.v1.DeploymentStrategy)
       :template                core.v1.PodTemplateSpec})
    (def apps.v1.DeploymentStatus {})
    (def apps.v1.Deployment
      {:kind       "Deployment"
       :apiVersion "v1"
       :metadata   meta.v1.ObjectMeta
       :spec       apps.v1.DeploymentSpec
       :status     apps.v1.DeploymentStatus}))
