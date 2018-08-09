(ns soil.events
  (:require [schema.core :as s]))

(def Event {:event s/Str
            (s/optional-key :data)  s/Any})

(s/defn deployment-created [name]
  {:event "DeploymentCreated"
   :data {:name name}})
