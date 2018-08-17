(ns soil.models.devspace
  (:require [schema.core :as s]
            [soil.models.application :as models.application]))

(s/defschema Devspace #:devspace{:name         s/Str
                                 :hive         models.application/Application
                                 :tanajura     models.application/Application
                                 :applications [models.application/Application]})

(s/defschema PersistentDevspace #:devspace {:name s/Str})
