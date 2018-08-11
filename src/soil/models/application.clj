(ns soil.models.application
  (:require [schema.core :as s]))

(s/defschema Container #:container{:name  s/Str
                                   :image s/Str
                                   :env   {s/Str s/Str}})

(s/defschema InterfaceType (s/enum :interface.type/http :interface.type/tcp :interface.type/udp))

(s/defschema Interface #:interface{:name      s/Str
                                   :port      s/Int
                                   :type      InterfaceType
                                   :container s/Str
                                   :host      s/Str})

(s/defschema ApplicationStatus (s/enum
                                 :application.status/template
                                 :application.status/creating
                                 :application.status/running
                                 :application.status/dead))

(s/defschema Application #:application{:name       s/Str
                                       :devspace   s/Str
                                       :containers [Container]
                                       :interfaces [Interface]
                                       :syncable?  s/Bool
                                       :status     ApplicationStatus})
