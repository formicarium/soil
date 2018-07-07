(ns soil.protocols.kubernetes.kubernetes-client)

(defprotocol KubernetesClient
  (create-namespace [this namespace]                             "Creates a Kubernetes namespace")
  (delete-namespace [this namespace-name]                        "Deletes a Kubernetes namespace")
  (list-namespaces [this]                                        "List Kubernetes namespaces")
  (create-deployment [this deployment]                           "Creates a Kubernetes deployment")
  (delete-deployment [this deployment-name deployment-namespace] "Deletes a Kubernetes deployment"))

(def IKubernetesClient (:on-interface KubernetesClient))
