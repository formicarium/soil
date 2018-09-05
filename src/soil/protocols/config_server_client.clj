(ns soil.protocols.config-server-client)

(defprotocol ConfigServerClient
  (on-create-devspace [this devspace-args] "Gets the set of Kubernetes entities to be created on a brand-new devspace")
  (on-create-service [this service-args] "Get `service` options for deploy"))

(def IConfigServerClient (:on-interface ConfigServerClient))
