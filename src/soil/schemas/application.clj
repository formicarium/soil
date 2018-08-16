(ns soil.schemas.application
  (:require [schema.core :as s]))

(s/defschema InterfaceType (s/enum :tcp :udp :http))
(s/defschema ApplicationDefinition {:name       s/Str
                                    :devspace   s/Str
                                    :containers [{:name  s/Str
                                                  :image s/Str
                                                  :syncable? s/Bool
                                                  :env   (s/pred map?)}]
                                    :interfaces [{:name      s/Str
                                                  :port      s/Int
                                                  :container s/Str
                                                  :type      InterfaceType}]})

(s/defschema ApplicationUrls {s/Keyword s/Str})
