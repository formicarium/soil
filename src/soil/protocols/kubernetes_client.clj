(ns soil.protocols.kubernetes-client)

(defprotocol KubernetesClient
  (list-nodes [this])

  (create-namespace! [this k8s-namespace])
  (list-namespaces [this] [this opts])
  (delete-namespace! [this namespace-name])
  (finalize-namespace! [this namespace-name])

  (get-ingress [this ingress-name namespace])
  (create-ingress! [this ingress])
  (list-ingresses [this namespace] [this namespace opts])
  (delete-ingress! [this ingress-name namespace])
  (delete-all-ingresses! [this namespace])

  (get-service [this service-name namespace])
  (create-service! [this service])
  (list-services [this namespace] [this namespace opts])
  (delete-service! [this service-name namespace])
  (delete-all-services! [this namespace])

  (get-deployment [this deployment-name namespace])
  (create-deployment! [this deployment])
  (list-deployment [this namespace] [this namespace opts])
  (delete-deployment! [this deployment-name namespace])
  (delete-all-deployments! [this namespace])

  (delete-service-account! [this service-account-name namespace])

  (get-config-map [this name namespace])
  (patch-config-map! [this name namespace config-map])

  (create-persistent-volume-claim! [this pvc])
  (get-persistent-volume-claim [this pvc-name namespace])

  (list-pods [this namespace] [this namespace opts]))

(def IKubernetesClient (:on-interface KubernetesClient))
