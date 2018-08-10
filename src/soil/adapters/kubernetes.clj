(ns soil.adapters.kubernetes
  (:require [schema.core :as s]
            [soil.models.kubernetes :as models.k8s]
            [soil.components.kubernetes.schema.deployment :as schema.k8s.deployment]
            [soil.components.kubernetes.schema.pod :as schema.k8s.pod]))

(s/defn externalize-port :- schema.k8s.pod/ContainerPort
  [{:port/keys [name container-port]} :- models.k8s/Port]
  {:name          name
   :containerPort container-port})

(s/defn internalize-port :- models.k8s/Port
  [{:keys [name containerPort]} :- schema.k8s.pod/ContainerPort]
  #:port {:name           name
          :container-port containerPort})

(s/defn externalize-container :- schema.k8s.pod/Container
  [{:container/keys [name image ports env]} :- models.k8s/Container]
  {:name  name
   :image image
   :ports (mapv externalize-port ports)
   :env   env})

(s/defn internalize-container :- models.k8s/Container
  [{:keys [name image ports env]} :- schema.k8s.pod/Container]
  #:container {:name  name
               :image image
               :ports (mapv internalize-port ports)
               :env   env})

(s/defn externalize-deployment :- schema.k8s.deployment/Deployment
  [{:deployment/keys [name namespace replicas containers]} :- models.k8s/Deployment]
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:name      name
                :labels    {:app name}
                :namespace namespace}
   :spec       {:selector {:matchLabels {:app name}}
                :replicas (or replicas 1)
                :template {:metadata {:name      name
                                      :namespace namespace
                                      :labels    {:app name}}
                           :spec     {:containers (mapv externalize-container containers)}}}})

(s/defn internalize-deployment :- models.k8s/Deployment
  [{{:keys [name namespace]}    :metadata
    {:keys [replicas template]} :spec} :- schema.k8s.deployment/Deployment]
  #:deployment {:name       name
                :namespace  namespace
                :replicas   replicas
                :containers (mapv internalize-container (get-in template [:spec :containers]))})

(s/defn externalize-service
  [{:service/keys [name namespace ports]} :- models.k8s/Service]
  {:apiVersion "v1"
   :kind       "Service"
   :metadata   {:name      name
                :labels    {:app name}
                :namespace namespace}
   :spec       {:ports    (mapv (fn [{:keys [service-port container-port name]}]
                                  {:protocol   "TCP"
                                   :name       name
                                   :port       service-port
                                   :targetPort container-port}) ports)
                :selector {:app name}}})

(s/defn internalize-service :- models.k8s/Service
  [{{:keys [name namespace]} :metadata
    {:keys [ports]} :spec}]
  #:service {:name name
             :namespace namespace
             :ports (mapv (fn [{:keys [name port targetPort]}]
                            {:name name
                             :service-port port
                             :container-port targetPort}) ports)})

(s/defn externalize-ingress-rule
  [{:rule/keys [host path service-name service-port]} :- models.k8s/IngressRule]
  {:host host
   :http {:paths [{:backend {:serviceName service-name
                             :servicePort service-port}
                   :path    path}]}})

(s/defn internalize-ingress-rule :- models.k8s/IngressRule
  [{:keys [host http]}]
  (let [path-obj (-> http :paths first)
        backend (-> path-obj :backend)]
    #:rule {:host host
            :path (-> path-obj :path)
            :service-name (-> backend :serviceName)
            :service-port (-> backend :servicePort)}))

(s/defn externalize-ingress
  [{:ingress/keys [name namespace rules]} :- models.k8s/Ingress]
  {:apiVersion "extensions/v1beta1"
   :kind       "Ingress"
   :metadata   {:name        name
                :annotations {"kubernetes.io/ingress.class" "nginx"}
                :labels      {:app name}
                :namespace   namespace}
   :spec       {:rules (mapv externalize-ingress-rule rules)}})

(s/defn internalize-ingress :- models.k8s/Ingress
  [{{:keys [name namespace]} :metadata
    {:keys [rules]} :spec}]
  #:ingress {:name      name
             :namespace namespace
             :rules     (mapv internalize-ingress-rule rules)})
