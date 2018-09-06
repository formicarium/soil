(ns soil.schemas.application
  (:require [schema.core :as s]
            [soil.models.application :as models.application]))

(s/defschema InterfaceType (s/enum :tcp :udp :http :https :nrepl))
(s/defschema ApplicationDefinition {:name                     s/Str
                                    :devspace                 s/Str
                                    :containers               [{:name                       s/Str
                                                                :image                      s/Str
                                                                (s/optional-key :syncable?) s/Bool
                                                                :env                        (s/pred map?)}]
                                    :interfaces               [{:name                     s/Str
                                                                :port                     s/Int
                                                                (s/optional-key :expose?) s/Bool
                                                                :container                s/Str
                                                                :type                     InterfaceType}]
                                    (s/optional-key :patches) [models.application/EntityPatch]})

(s/defschema DevspacedApplicationDefinition (dissoc ApplicationDefinition :devspace :name))

(s/defschema ApplicationUrls {s/Keyword s/Str})
(s/defschema Application {:name     s/Str
                          :devspace s/Str
                          :links    ApplicationUrls})

