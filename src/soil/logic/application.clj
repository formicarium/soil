(ns soil.logic.application
  (:require [clj-service.protocols.config :as protocols.config]
            [soil.logic.interface :as logic.interface]
            [schema.core :as s]
            [soil.models.application :as models.application]))

(s/defn hive-application :- models.application/Application
  [devspace-name :- s/Str
   config :- protocols.config/IConfig]
  (let [hive-image   (protocols.config/get-in! config [:hive :image])
        domain       (protocols.config/get-in! config [:formicarium :domain])
        service-name "hive"]
    #:application{:name       service-name
                  :devspace   devspace-name
                  :containers [#:container{:name  service-name
                                           :image hive-image
                                           :env   {}}]
                  :interfaces [(logic.interface/new {:name      "default"
                                                     :port      8080
                                                     :container service-name
                                                     :devspace  devspace-name
                                                     :service   service-name
                                                     :domain    domain})
                               (logic.interface/new {:name      "repl"
                                                     :port      2222
                                                     :container service-name
                                                     :type      :interface.type/tcp
                                                     :devspace  devspace-name
                                                     :service   service-name
                                                     :domain    domain})
                               (logic.interface/new {:name      "zmq"
                                                     :port      9898
                                                     :type      :interface.type/tcp
                                                     :container service-name
                                                     :devspace  devspace-name
                                                     :service   service-name
                                                     :domain    domain})]
                  :syncable?  false
                  :status     :application.status/template}))
