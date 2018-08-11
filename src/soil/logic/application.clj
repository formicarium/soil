(ns soil.logic.application
  (:require [clj-service.protocols.config :as protocols.config]
            [soil.logic.interface :as logic.interface]
            [schema.core :as s]
            [soil.models.application :as models.application]
            [soil.schemas.application :as schemas.application]))

(s/defn hive-definition :- schemas.application/ApplicationDefinition
  [devspace :- s/Str
   config :- protocols.config/IConfig]
  (let [hive-image   (protocols.config/get-in! config [:hive :image])
        service-name "hive"]
    {:name       service-name
     :devspace   devspace
     :syncable?  false
     :containers [{:name  service-name
                   :image hive-image
                   :env   {}}]
     :interfaces [{:name      :default
                   :port      8080
                   :type      :http
                   :container service-name}
                  {:name      "repl"
                   :port      2222
                   :type      :tcp
                   :container service-name}
                  {:name      "zmq"
                   :port      9898
                   :type      :tcp
                   :container service-name}]}))

(s/defn tanajura-definition :- schemas.application/ApplicationDefinition
  [devspace :- s/Str
   config :- protocols.config/IConfig]
  (let [tanajura-image (protocols.config/get-in! config [:tanajura :image])
        service-name   "tanajura"]
    {:name       service-name
     :devspace   devspace
     :syncable?  false
     :containers [{:name  service-name
                   :image tanajura-image
                   :env   {}}]
     :interfaces [{:name      :default
                   :port      3002
                   :type      :http
                   :container service-name}
                  {:name      "git"
                   :port      6666
                   :type      :http
                   :container service-name}]}))
