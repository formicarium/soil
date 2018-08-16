(ns soil.schemas.devspace
  (:require [schema.core :as s]
            [soil.schemas.application :as schemas.application]))

(s/defschema CreateDevspace {:name s/Str})

(s/defschema Devspace {:name         s/Str
                       :hive         schemas.application/ApplicationUrls
                       :tanajura     schemas.application/ApplicationUrls
                       :applications [schemas.application/ApplicationUrls]})
