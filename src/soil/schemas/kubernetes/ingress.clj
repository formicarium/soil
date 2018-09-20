(ns soil.schemas.kubernetes.ingress
  (:require [schema.core :as s]))

(s/defschema Ingress (s/pred map?))
