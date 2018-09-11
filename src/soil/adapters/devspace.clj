(ns soil.adapters.devspace
  (:require [schema.core :as s]
            [soil.schemas.kubernetes.namespace :as schemas.k8s.namespace]
            [soil.config :as config]
            [soil.schemas.devspace :as schemas.devspace]
            [soil.models.devspace :as models.devspace]
            [soil.adapters.application :as adapters.application]
            [clj-service.misc :as misc]
            [soil.models.application :as models.application]
            [soil.logic.application :as logic.application]
            [clj-service.protocols.config :as protocols.config]))

(s/defn devspace-name->create-namespace :- schemas.k8s.namespace/CreateNamespace
  [devspace-name :- s/Str]
  {:metadata
   {:name   devspace-name
    :labels {:formicarium.io/kind config/fmc-devspace-label}}})

(s/defn internal->wire :- schemas.devspace/Devspace
  [devspace :- models.devspace/Devspace]
  (-> (misc/map-keys (comp keyword name) devspace)
      (update :hive adapters.application/internal->wire)
      (update :tanajura adapters.application/internal->wire)
      (update :applications #(mapv adapters.application/internal->wire %))))

(s/defn devspace-name->application-prefix
  [devspace-name :- s/Str]
  (str "applications/" devspace-name))

(s/defn devspace-name->key :- s/Str
  [devspace-name :- s/Str]
  (str "devspaces/" devspace-name))

(s/defn devspace-name->persistent :- models.devspace/PersistentDevspace
  [devspace-name :- s/Str]
  #:devspace{:name devspace-name})

(s/defn persistent+applications->internal :- models.devspace/Devspace
  [devspace :- models.devspace/PersistentDevspace
   applications :- [models.application/Application]]
  (assoc devspace
    :devspace/hive (logic.application/get-hive applications)
    :devspace/tanajura (logic.application/get-tanajura applications)
    :devspace/applications (logic.application/but-hive-tanajura applications)))

(s/defn create-devspace->args-map :- (s/pred map?)
  [create-devspace :- schemas.devspace/CreateDevspace]
  (merge {:name (:name create-devspace)} (:args create-devspace)))

(s/defn create-devspace->applications? :- (s/maybe [models.application/Application])
  [create-devspace :- schemas.devspace/CreateDevspace
   config :- protocols.config/IConfig]
  (some->> create-devspace
           :setup
           (mapv #(adapters.application/definition->application % config))))
