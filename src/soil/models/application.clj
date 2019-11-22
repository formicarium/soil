(ns soil.models.application
  (:require [schema.core :as s]))


(s/defschema Patch {:op     (s/enum "add" "remove" "replace" "move" "copy" "test")
                    :path   s/Str
                    (s/optional-key :from) s/Str
                    :value  s/Any})

(s/defschema EntityPatch {:kind (s/enum "Deployment" "Service" "Ingress")
                          :patch Patch})

(s/defschema SyncableCode {:syncable-code/name s/Str})

(s/defschema Container {:container/name                            s/Str
                        :container/image                           s/Str
                        (s/optional-key :container/syncable?)      s/Bool
                        (s/optional-key :container/syncable-codes) #{SyncableCode}
                        :container/env                             {s/Str s/Str}})

(s/defschema InterfaceType (s/enum
                             :interface.type/http
                             :interface.type/https
                             :interface.type/tcp
                             :interface.type/nrepl
                             :interface.type/udp))

(s/defschema Interface {:interface/name                     s/Str ;; TODO: MUST HAVE LESS THAN 15 CHARS - KUBERNETES REQUIREMENT
                        :interface/port                     s/Int
                        (s/optional-key :interface/expose?) s/Bool
                        :interface/type                     InterfaceType
                        :interface/container                s/Str
                        :interface/host                     s/Str})

(s/defschema Application #:application{:name       s/Str
                                       :service    s/Str
                                       :devspace   s/Str
                                       (s/optional-key :args) (s/pred map?)
                                       :containers [Container]
                                       :interfaces [Interface]
                                       :patches    [EntityPatch]})
