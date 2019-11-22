(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.models.devspace :as models.devspace]
            [soil.adapters.application :as adapters.application]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [clj-service.protocols.config :as protocols.config]
            [schema.core :as s]
            [selmer.parser]
            [soil.models.application :as models.application]
            [soil.controllers.application :as controllers.application]
            [clojure.java.io :as io]
            [soil.schemas.devspace :as schemas.devspace]
            [clj-service.exception :as exception]
            [soil.diplomat.config-server :as diplomat.config-server]
            [soil.adapters.devspace :as adapters.devspace]
            [io.pedestal.log :as log]))

(s/defn ^:private load-application-template :- models.application/Application
  [name :- s/Str
   replace-map :- (s/pred map?)
   config :- protocols.config/IConfig]
  (-> (str "templates/" name ".edn")
      io/resource
      slurp
      (selmer.parser/render replace-map)
      read-string
      (adapters.application/definition->application {} config)))

(s/defn hive-application :- models.application/Application
  [devspace :- s/Str
   config :- protocols.config/IConfig]
  (load-application-template "hive" {:devspace devspace} config))

(s/defn tanajura-application :- models.application/Application
  [devspace :- s/Str
   config :- protocols.config/IConfig]
  (load-application-template "tanajura" {:devspace devspace
                                         :soil-url (protocols.config/get-in! config [:soil :url])} config))

(s/defn create-tanajura! :- models.application/Application
  [devspace-name :- s/Str
   config :- protocols.config/IConfig
   k8s-client :- protocols.k8s/IKubernetesClient]
  (let [tanajura (tanajura-application devspace-name config)
        tanajura-storage-size (protocols.config/get-in-maybe config [:tanajura :storage-size])
        tanajura-storage-class (protocols.config/get-in-maybe config [:tanajura :storage-class-name])]
    (if tanajura-storage-size
      (do (diplomat.kubernetes/create-persistent-volume-claim! tanajura-storage-size tanajura-storage-class tanajura k8s-client)
          (controllers.application/create-application! tanajura config k8s-client))
      (do (log/info :log ::skipping-persistent-volume-claim)
          (controllers.application/create-application! (if tanajura-storage-size
                                                         tanajura
                                                         (disj tanajura :application/patches))
                                                       config
                                                       k8s-client)))))

(s/defn create-setup! :- [models.application/Application]
  [create-devspace :- schemas.devspace/CreateDevspace
   config :- protocols.config/IConfig
   config-server :- soil.protocols.config-server-client/IConfigServerClient
   k8s-client :- protocols.k8s/IKubernetesClient]
  (log/info :log :creating-setup)
  (->> (or (adapters.devspace/create-devspace->applications? create-devspace config)
           (diplomat.config-server/get-devspace-applications create-devspace config config-server))
       (mapv #(controllers.application/create-application! % config k8s-client))))

(s/defn create-devspace! :- models.devspace/Devspace
  [{devspace-name :name devspace-args :args :as new-devspace} :- schemas.devspace/CreateDevspace
   config :- protocols.config/IConfig
   config-server :- soil.protocols.config-server-client/IConfigServerClient
   k8s-client :- protocols.k8s/IKubernetesClient]
  (diplomat.kubernetes/create-namespace! devspace-name devspace-args k8s-client)
  #:devspace{:name         devspace-name
             :hive         (-> (hive-application devspace-name config)
                               (controllers.application/create-application! config k8s-client))
             :tanajura     (create-tanajura! devspace-name config k8s-client)
             :applications (create-setup! new-devspace config config-server k8s-client)})


(s/defn get-devspaces :- [models.devspace/Devspace]
  [k8s-client :- protocols.k8s/IKubernetesClient]
  (->> (diplomat.kubernetes/get-devspaces-names k8s-client)
       (mapv #(diplomat.kubernetes/get-devspace % k8s-client))))

(s/defn one-devspace :- models.devspace/Devspace
  [devspace-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (diplomat.kubernetes/get-devspace devspace-name k8s-client))

(s/defn check-if-devspace-exists :- (s/maybe models.devspace/Devspace)
  [devspace-name :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  ((->> (diplomat.kubernetes/get-devspaces-names k8s-client)
        set) devspace-name))

(s/defn delete-devspace!
  [devspace :- s/Str
   k8s-client :- protocols.k8s/IKubernetesClient]
  (if-let [deleteable-devspace (check-if-devspace-exists devspace k8s-client)]
    (protocols.k8s/delete-namespace! k8s-client deleteable-devspace)
    (exception/not-found! {:log (str "Devspace " devspace " does not exist")})))

