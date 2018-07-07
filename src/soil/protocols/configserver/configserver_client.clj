(ns soil.protocols.configserver.configserver-client
  (:require [schema.core :as s]))

(defprotocol ConfigServerClient
  (on-startup-environment [this env] "Gets the set of Kubernetes entities to be created on a brand-new environment")
  (on-deploy-service [this service-args] "Get `service` options for deploy"))

(def IConfigServerClient (:on-interface ConfigServerClient))
