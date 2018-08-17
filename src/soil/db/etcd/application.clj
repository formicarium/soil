(ns soil.db.etcd.application
  (:require [soil.protocols.etcd :as protocols.etcd]
            [schema.core :as s]
            [soil.models.application :as models.application]
            [soil.adapters.application :as adapters.application]))

(s/defn create-application! :- models.application/Application
  [application :- models.application/Application
   etcd :- protocols.etcd/IEtcd]
  (protocols.etcd/put! etcd (adapters.application/application->key application) application)
  application)

(s/defn get-application! :- models.application/Application
  [application-name :- s/Str
   devspace :- s/Str
   etcd :- protocols.etcd/IEtcd]
  (protocols.etcd/get! etcd (adapters.application/application-key devspace application-name)))
