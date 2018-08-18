(ns soil.models.application
  (:require [schema.core :as s]))

(s/defschema Container {:container/name                       s/Str
                        :container/image                      s/Str
                        (s/optional-key :container/syncable?) s/Bool
                        :container/env                        {s/Str s/Str}})

(s/defschema InterfaceType (s/enum
                             :interface.type/http
                             :interface.type/https
                             :interface.type/tcp
                             :interface.type/nrepl
                             :interface.type/udp))

(s/defschema Interface {:interface/name                     s/Str
                        :interface/port                     s/Int
                        (s/optional-key :interface/expose?) s/Bool
                        :interface/type                     InterfaceType
                        :interface/container                s/Str
                        :interface/host                     s/Str})

(s/defschema ApplicationStatus (s/enum
                                 :application.status/template
                                 :application.status/creating
                                 :application.status/running
                                 :application.status/dead))

(s/defschema Application #:application{:name       s/Str
                                       :devspace   s/Str
                                       :containers [Container]
                                       :interfaces [Interface]
                                       :status     ApplicationStatus})
