(ns soil.diplomat.config-server
  (:require [schema.core :as s]
            [soil.models.application :as models.application]
            [soil.schemas.service :as schemas.service]
            [soil.protocols.config-server-client :as protocols.config-server-client]
            [soil.adapters.application :as adapters.application]
            [soil.adapters.service :as adapters.service]
            [clj-service.protocols.config :as protocols.config]
            [soil.adapters.devspace :as adapters.devspace]
            [soil.schemas.devspace :as schemas.devspace]))

(s/defn get-service-application :- models.application/Application
  [devspace-name :- s/Str
   devspace-args :- (s/pred map?)
   {:keys [args] :or {args {}} :as service-deploy} :- schemas.service/DeployService
   config :- protocols.config/IConfig
   config-server :- protocols.config-server-client/IConfigServerClient]
  (->> (adapters.service/devspace+service-deploy->args-map devspace-name devspace-args service-deploy)
       (protocols.config-server-client/on-create-service config-server)
       (map #(adapters.application/definition->application % args config))))

(s/defn get-devspace-applications :- [models.application/Application]
  [{:keys [args] :or {args {}} :as create-devspace} :- schemas.devspace/CreateDevspace
   config :- protocols.config/IConfig
   config-server :- protocols.config-server-client/IConfigServerClient]
  (->> (adapters.devspace/create-devspace->args-map create-devspace)
       (protocols.config-server-client/on-create-devspace config-server)
       (map #(adapters.application/definition->application % args config))))
