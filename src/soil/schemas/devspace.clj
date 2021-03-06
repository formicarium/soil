(ns soil.schemas.devspace
  (:require [schema.core :as s]
            [soil.schemas.application :as schemas.application]))

(s/defschema SetupAppDefinition (dissoc schemas.application/ApplicationDefinition :devspace))
(s/defschema CreateDevspace {:name                   s/Str
                             (s/optional-key :args)  (s/maybe (s/pred map?))
                             (s/optional-key :setup) (s/maybe [SetupAppDefinition])})

(s/defschema Devspace {:name         s/Str
                       :hive         schemas.application/Application
                       :tanajura     schemas.application/Application
                       :applications [schemas.application/Application]})
