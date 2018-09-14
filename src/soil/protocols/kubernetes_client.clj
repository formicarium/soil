(ns soil.protocols.kubernetes-client)

(defprotocol KubernetesClient
  (list-nodes [this] "Lists Kubernetes nodes")

  (create-namespace! [this k8s-namespace] "Creates a Kubernetes namespace")
  (list-namespaces [this] [this opts] "List Kubernetes namespaces")
  (delete-namespace! [this namespace-name] "Deletes a Kubernetes namespace")

  (create-ingress! [this ingress] "Creates a Kubernetes ingress")
  (delete-ingress! [this ingress-name namespace] "Delete a Kubernetes ingress")
  (list-ingresses [this namespace opts] "List ingresses")

  (create-service! [this service] "Creates a Kubernetes Service")
  (get-service [this service-name namespace] "Get a Kubernetes Service")
  (delete-service! [this service-name namespace] "Delete a Kubernetes Service")
  (list-services [this namespace] [this namespace opts] "List service")

  (create-deployment! [this deployment] "Creates a Kubernetes deployment")
  (list-deployment [this namespace] [this namespace opts] "List Kubernetes deployments")
  (delete-deployment! [this deployment-name namespace] "Deletes a Kubernetes deployment")

  (get-config-map [this name namespace] "Get a Kubernetes ConfigMap")
  (patch-config-map! [this name namespace config-map] "Patch a Kubernetes ConfigMap")

  (list-pods [this namespace] [this namespace opts] "List Kubernetes Pods"))

(def IKubernetesClient (:on-interface KubernetesClient))
