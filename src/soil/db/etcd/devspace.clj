(ns soil.db.etcd.devspace
  (:require [schema.core :as s]
            [soil.protocols.etcd :as protocols.etcd]
            [soil.models.devspace :as models.devspace]
            [soil.adapters.devspace :as adapters.devspace]))

(s/defn create-devspace!
  [devspace-name :- s/Str
   etcd :- protocols.etcd/IEtcd]
  (protocols.etcd/put! etcd
    (adapters.devspace/devspace-name->key devspace-name)
    (adapters.devspace/devspace-name->persistent devspace-name)))

(s/defn get-devspaces :- [models.devspace/Devspace]
  [])

(s/defn list-persistent-devspaces! :- [models.devspace/PersistentDevspace]
  [etcd :- protocols.etcd/IEtcd]
  (protocols.etcd/get-prefix! etcd "devspaces/"))


(s/defn delete-devspace!
  [devspace-name :- s/Str
   etcd :- protocols.etcd/IEtcd]
  (protocols.etcd/delete-prefix! etcd (adapters.devspace/devspace-name->key devspace-name)))
