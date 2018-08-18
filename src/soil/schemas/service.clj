(ns soil.schemas.service
  (:require [schema.core :as s]
            [soil.schemas.application :as schemas.application]))

(s/defschema DeployService {:name                        s/Str
                            :args                        (s/pred map?)
                            (s/optional-key :definition) schemas.application/ApplicationDefinition})
