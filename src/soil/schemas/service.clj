(ns soil.schemas.service
  (:require [schema.core :as s]))

(s/defschema DeployService {:name     s/Str
                            s/Keyword s/Any})
