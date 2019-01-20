(ns soil.adapters.service
  (:require [schema.core :as s]
            [soil.models.application :as models.application]
            [soil.schemas.service :as schemas.service]
            [soil.adapters.definition :as adapters.definition]
            [soil.adapters.application :as adapters.application]
            [clj-service.protocols.config :as protocols.config]))

(s/defn service-deploy+devspace->application? :- (s/maybe [models.application/Application])
  [{:keys [definition args] service-name :name :as service-deploy} :- schemas.service/DeployService
   devspace :- s/Str
   config :- protocols.config/IConfig]
  (some-> definition
          (adapters.definition/devspaced-app-definition->app-defintion devspace service-name)
          (adapters.application/definition->application args config)
          vector))

(s/defn devspace+service-deploy->args-map :- (s/pred map?)
  [devspace-name :- s/Str
   devspace-args :- (s/pred map?)
   {:keys [args syncable definition] :or {args {}} :as deploy-service} :- schemas.service/DeployService]
  (merge
    args
    {:devspace {:name devspace-name
                :args devspace-args}
     :name     (:name deploy-service)
     :args     args
     :local    syncable}))
