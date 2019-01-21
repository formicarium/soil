(ns soil.schemas.kubernetes.namespace
  (:require [schema.core :as s]))

(s/defschema CreateNamespace {:metadata {:name   s/Str
                                         :labels {(s/eq "formicarium.io/kind") s/Str}}})
