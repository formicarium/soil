(ns soil.protocols.kubernetes-client)

(defprotocol KubernetesClient
  (list-nodes [this]                                          "Lists Kubernetes nodes")

  (create-namespace! [this k8s-namespace]                     "Creates a Kubernetes namespace")
  (list-namespaces [this]                                     "List Kubernetes namespaces")
  (delete-namespace! [this namespace-name]                    "Deletes a Kubernetes namespace")

  (create-ingress! [this ingress]                             "Creates a Kubernetes ingress")
  (delete-ingress! [this ingress-name namespace]              "Delete a Kubernetes ingress")

  (create-service! [this service]                             "Creates a Kubernetes Service")
  (get-service [this service-name namespace]                  "Get a Kubernetes Service")
  (delete-service! [this service-name namespace]              "Delete a Kubernetes Service")

  (create-deployment! [this deployment]                       "Creates a Kubernetes deployment")
  (list-deployment [this deployment]                           "List Kubernetes deployments")
  (delete-deployment! [this deployment-name namespace]        "Deletes a Kubernetes deployment")

  (get-config-map [this name namespace]                       "Get a Kubernetes ConfigMap")
  (patch-config-map! [this name namespace config-map]         "Patch a Kubernetes ConfigMap")

  (list-pods [this namespace]                                 "List Kubernetes Pods"))

(def IKubernetesClient (:on-interface KubernetesClient))
