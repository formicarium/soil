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

(s/defn persistent-devspace->devspace :- models.devspace/Devspace
  [{:devspace/keys [name] :as devspace} :- models.devspace/PersistentDevspace
   etcd :- protocols.etcd/IEtcd]
  (->> (str "applications/" name)
       (protocols.etcd/get-prefix! etcd)
       (mapv :value)
       (adapters.devspace/persistent+applications->internal devspace)))

(s/defn get-devspaces :- [models.devspace/Devspace]
  [etcd :- protocols.etcd/IEtcd]
  (->> (protocols.etcd/get-prefix! etcd "devspaces")
       (mapv :value)
       (mapv #(persistent-devspace->devspace % etcd))))

(s/defn get-devspace :- models.devspace/Devspace
  [devspace-name :- s/Str
   etcd :- protocols.etcd/IEtcd]
  (-> (protocols.etcd/get! etcd (adapters.devspace/devspace-name->key devspace-name))
      :value
      (persistent-devspace->devspace etcd)))
