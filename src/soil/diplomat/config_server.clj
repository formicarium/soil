(ns soil.diplomat.config-server
  (:require [schema.core :as s]
            [soil.models.application :as models.application]
            [soil.schemas.service :as schemas.service]
            [soil.protocols.config-server-client :as protocols.config-server-client]
            [soil.adapters.application :as adapters.application]
            [clj-service.protocols.config :as protocols.config]
            [clj-service.schema :as schema]
            [soil.schemas.application :as schemas.application]))

(s/defn get-service-application :- models.application/Application
  [devspace :- s/Str
   service-deploy :- schemas.service/DeployService
   config :- protocols.config/IConfig
   config-server :- protocols.config-server-client/IConfigServerClient]
  (-> (protocols.config-server-client/on-deploy-service config-server service-deploy)
      (assoc :devspace devspace)
      (schema/coerce schemas.application/ApplicationDefinition)
      (adapters.application/definition->application config)))
