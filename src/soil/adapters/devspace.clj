(ns soil.adapters.devspace
  (:require [schema.core :as s]
            [soil.schemas.kubernetes.namespace :as schemas.k8s.namespace]
            [soil.config :as config]
            [soil.schemas.devspace :as schemas.devspace]
            [soil.models.devspace :as models.devspace]
            [soil.adapters.application :as adapters.application]
            [clj-service.misc :as misc]))

(s/defn devspace-name->create-namespace :- schemas.k8s.namespace/CreateNamespace
  [devspace-name :- s/Str]
  {:metadata
   {:name   devspace-name
    :labels {:kind config/fmc-devspace-label}}})

(s/defn internal->wire :- schemas.devspace/Devspace
  [devspace :- models.devspace/Devspace]
  (-> (misc/map-keys (comp keyword name) devspace)
      (update :hive adapters.application/application->urls)
      (update :tanajura adapters.application/application->urls)
      (update :applications #(mapv adapters.application/application->urls %))))
