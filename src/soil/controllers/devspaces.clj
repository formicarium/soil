(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.models.devspace :as models.devspace]
            [soil.logic.devspace :as logic.devspace]
            [soil.logic.services :as logic.service]
            [soil.controllers.services :as controllers.services]
            [soil.adapters.application :as adapters.application]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [clj-service.protocols.config :as protocols.config]
            [schema.core :as s]
            [selmer.parser]
            [soil.models.application :as models.application]
            [soil.controllers.application :as controllers.application]
            [clojure.java.io :as io]
            [soil.db.etcd.devspace :as etcd.devspace]
            [soil.protocols.etcd :as protocols.etcd]
            [soil.db.etcd.application :as etcd.application]
            [soil.schemas.devspace :as schemas.devspace]
            [clj-service.exception :as exception]
            [soil.diplomat.config-server :as diplomat.config-server]
            [soil.adapters.devspace :as adapters.devspace]))

(s/defn ^:private load-application-template :- models.application/Application
  [name :- s/Str
   replace-map :- (s/pred map?)
   config :- protocols.config/IConfig]
  (-> (str "templates/" name ".edn")
      io/resource
      slurp
      (selmer.parser/render replace-map)
      read-string
      (adapters.application/definition->application config)))

(s/defn hive-application :- models.application/Application
  [devspace :- s/Str
   config :- protocols.config/Config]
  (load-application-template "hive" {:devspace devspace} config))

(s/defn tanajura-application :- models.application/Application
  [devspace :- s/Str
   config :- protocols.config/Config]
  (load-application-template "tanajura" {:devspace devspace} config))

(s/defn create-setup! :- [models.application/Application]
  [create-devspace :- schemas.devspace/CreateDevspace
   config :- protocols.config/IConfig
   config-server :- soil.protocols.config-server-client/IConfigServerClient
   etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (or (adapters.devspace/create-devspace->applications? create-devspace config)
           (diplomat.config-server/get-devspace-applications create-devspace config config-server))
       (mapv #(controllers.application/create-application! % etcd config k8s-client))))

(s/defn render-devspace :- models.devspace/Devspace
  [devspace :- models.devspace/Devspace
   k8s-client :- protocols.k8s/KubernetesClient]
  (-> devspace
      (update-in [:devspace/hive] #(controllers.application/render-application % k8s-client))
      (update-in [:devspace/tanajura] #(controllers.application/render-application % k8s-client))
      (update-in [:devspace/applications] (fn [apps] (mapv #(controllers.application/render-application % k8s-client) apps)))))

(s/defn create-devspace! :- models.devspace/Devspace
  [{devspace-name :name :as new-devspace} :- schemas.devspace/CreateDevspace
   config :- protocols.config/IConfig
   config-server :- soil.protocols.config-server-client/IConfigServerClient
   etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s/IKubernetesClient]
  (diplomat.kubernetes/create-namespace! devspace-name k8s-client)
  (etcd.devspace/create-devspace! devspace-name etcd)
  #:devspace{:name         devspace-name
             :hive         (-> (hive-application devspace-name config)
                               (controllers.application/create-application! etcd config k8s-client))
             :tanajura     (-> (tanajura-application devspace-name config)
                               (controllers.application/create-application! etcd config k8s-client))
             :applications (create-setup! new-devspace config config-server etcd k8s-client)})


(s/defn get-devspaces :- [models.devspace/Devspace]
  [etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s/KubernetesClient]
  (mapv #(render-devspace % k8s-client) (etcd.devspace/get-devspaces etcd)))

(s/defn one-devspace :- models.devspace/Devspace
  [devspace-name :- s/Str
   etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s/KubernetesClient]
  (-> (etcd.devspace/get-devspace devspace-name etcd)
      (render-devspace k8s-client)))

(s/defn check-if-devspace-exists :- (s/maybe models.devspace/Devspace)
  [devspace-name :- s/Str
   etcd :- protocols.etcd/IEtcd]
  ((->> (etcd.devspace/get-devspaces etcd)
        (mapv :devspace/name)
        set) devspace-name))

(s/defn delete-devspace!
  [devspace :- s/Str
   etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s/IKubernetesClient]
  (if-let [deleteable-devspace (check-if-devspace-exists devspace etcd)]
    (do
      (protocols.k8s/delete-namespace! k8s-client deleteable-devspace)
      (etcd.devspace/delete-devspace! deleteable-devspace etcd)
      (etcd.application/delete-all-applications! deleteable-devspace etcd))
    (exception/not-found! {:log (str "Devspace " devspace " does not exist")})))

