(ns soil.schemas.kubernetes.namespace
  (:require [schema.core :as s]))

(s/defschema CreateNamespace {:metadata {:name   s/Str
                                         :labels {:kind s/Str}}})
