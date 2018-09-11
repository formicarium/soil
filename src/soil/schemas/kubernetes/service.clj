(ns soil.schemas.kubernetes.service
  (:require [schema.core :as s]))

(s/defschema Service (s/pred map?))
