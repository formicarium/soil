(ns soil.protocols.kubernetes.kubernetes-client)

(defprotocol KubernetesClient
  (create-namespace [this namespace] [this namespace labels] "Creates a Kubernetes namespace")
  (list-namespaces [this]                                    "List Kubernetes namespaces")
  (delete-namespace [this namespace-name]                    "Deletes a Kubernetes namespace")

  (create-ingress [this ingress]                             "Creates a Kubernetes ingress")
  (delete-ingress [this ingress-name namespace]              "Delete a Kubernetes ingress")

  (create-service [this service]                             "Creates a Kubernetes service")
  (delete-service [this service-name namespace]              "Delete a Kubernetes service")

  (create-deployment [this deployment]                       "Creates a Kubernetes deployment")
  (delete-deployment [this deployment-name namespace]        "Deletes a Kubernetes deployment"))

(def IKubernetesClient (:on-interface KubernetesClient))
