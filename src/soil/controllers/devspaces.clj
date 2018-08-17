(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.models.devspace :as models.devspace]
            [soil.logic.devspace :as logic.devspace]
            [soil.logic.services :as logic.service]
            [soil.adapters.application :as adapters.application]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [clj-service.protocols.config :as protocols.config]
            [schema.core :as s]
            [selmer.parser]
            [soil.models.application :as models.application]
            [soil.controllers.application :as controllers.application]
            [clojure.java.io :as io]
            [soil.db.etcd.devspace :as etcd.devspace]
            [soil.protocols.etcd :as protocols.etcd]))

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

(s/defn create-devspace! :- models.devspace/Devspace
  [devspace-name :- s/Str
   config :- protocols.config/IConfig
   etcd :- protocols.etcd/IEtcd
   k8s-client :- protocols.k8s/IKubernetesClient]
  (let [hive-app     (hive-application devspace-name config)
        tanajura-app (tanajura-application devspace-name config)]
    (diplomat.kubernetes/create-namespace! devspace-name k8s-client)
    (etcd.devspace/create-devspace! devspace-name etcd)
    (controllers.application/create-application! hive-app etcd k8s-client)
    (controllers.application/create-application! tanajura-app etcd k8s-client)
    #:devspace{:name         devspace-name
               :hive         hive-app
               :tanajura     tanajura-app
               :applications []}))

(s/defn get-devspaces :- [models.devspace/Devspace]
  [etcd :- protocols.etcd/IEtcd]
  (etcd.devspace/get-devspaces etcd))

(s/defn one-devspace :- models.devspace/Devspace
  [devspace-name :- s/Str
   etcd :- protocols.etcd/IEtcd]
  (etcd.devspace/get-devspace devspace-name etcd))

(s/defn delete-devspace!
  [devspace :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (protocols.k8s/delete-namespace! k8s-client devspace))

