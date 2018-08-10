(ns soil.models.kubernetes
  (:require [schema.core :as s]))

(s/defschema Port #:port {:name s/Str :container-port s/Int})

(s/defschema Container #:container {:name  s/Str
                                    :image s/Str
                                    :ports [Port]
                                    :env   [{:name  s/Str
                                             :value s/Str}]})

(s/defschema Deployment {:deployment/name           s/Str
                         :deployment/namespace      s/Str
                         (s/optional-key :deployment/replicas) (s/maybe s/Int)
                         :deployment/containers     [Container]})

(s/defschema Service #:service {:name      s/Str
                                :namespace s/Str
                                :ports     [{:service-port   s/Int
                                             :container-port (s/either s/Str s/Int)
                                             :name           s/Str}]})

(s/defschema IngressRule #:rule {:host         s/Str
                                 :path         s/Str
                                 :service-name s/Str
                                 :service-port (s/either s/Str s/Int)})

(s/defschema Ingress #:ingress {:name      s/Str
                                :namespace s/Str
                                :rules     [IngressRule]})
