(ns soil.schemas.service
  (:require [schema.core :as s]
            [soil.schemas.application :as schemas.application]))

(s/defschema DeployService {:name                        s/Str
                            (s/optional-key :args)       (s/maybe (s/pred map?))
                            (s/optional-key :syncable)   (s/maybe s/Bool)
                            (s/optional-key :definition) (s/maybe schemas.application/DevspacedApplicationDefinition)})

(s/defschema DeploySet {:services DeployService})
