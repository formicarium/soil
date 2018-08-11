(ns soil.adapters.devspace
  (:require [schema.core :as s]
            [soil.schemas.kubernetes.namespace :as schemas.k8s.namespace]
            [soil.config :as config]))

(s/defn devspace-name->create-namespace :- schemas.k8s.namespace/CreateNamespace
  [devspace-name :- s/Str]
  {:metadata
   {:name   devspace-name
    :labels {:kind config/fmc-devspace-label}}})
