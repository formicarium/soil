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
  [etcd :- protocols.etcd/IEtcd]
  (->> (protocols.etcd/get-prefix! etcd "devspaces")
       (mapv :value)
       (mapv (fn [{:devspace/keys [name] :as devspace}]
               (->> (str "applications/" name)
                    (protocols.etcd/get-prefix! etcd)
                    (mapv :value)
                    (adapters.devspace/persistent+applications->internal devspace))))))

(s/defn list-persistent-devspaces! :- [models.devspace/PersistentDevspace]
  [etcd :- protocols.etcd/IEtcd]
  (protocols.etcd/get-prefix! etcd "devspaces/"))
