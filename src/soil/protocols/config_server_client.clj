(ns soil.protocols.config-server-client)

(defprotocol ConfigServerClient
  (on-new-devspace [this devspace] "Gets the set of Kubernetes entities to be created on a brand-new devspace")
  (on-deploy-service [this service-args] "Get `service` options for deploy"))

(def IConfigServerClient (:on-interface ConfigServerClient))
