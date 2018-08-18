(ns soil.adapters.definition
  (:require [schema.core :as s]
            [soil.schemas.application :as schemas.application]))

(s/defn devspaced-app-definition->app-defintion :- schemas.application/ApplicationDefinition
  [definition :- schemas.application/DevspacedApplicationDefinition
   devspace-name :- s/Str
   service-name :- s/Str]
  (assoc definition :devspace devspace-name
                    :name service-name))
