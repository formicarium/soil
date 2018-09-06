(ns soil.adapters.service
  (:require [schema.core :as s]
            [soil.models.application :as models.application]
            [soil.schemas.service :as schemas.service]
            [soil.adapters.definition :as adapters.definition]
            [soil.adapters.application :as adapters.application]
            [clj-service.protocols.config :as protocols.config]))

(s/defn service-deploy+devspace->application? :- (s/maybe [models.application/Application])
  [service-deploy :- schemas.service/DeployService
   devspace :- s/Str
   config :- protocols.config/IConfig]
  (some-> service-deploy
          :definition
          (adapters.definition/devspaced-app-definition->app-defintion devspace (:name service-deploy))
          (adapters.application/definition->application config)
          vector))

(s/defn devspace+service-deploy->args-map :- (s/pred map?)
  [devspace :- s/Str
   service-deploy :- schemas.service/DeployService]
  (merge {:devspace devspace
          :name     (:name service-deploy)}
         (:args service-deploy)))
