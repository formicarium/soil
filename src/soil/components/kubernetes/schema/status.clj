(ns soil.components.kubernetes.schema.status
  (:require [schema.core :as s]))

#_(s/defschema Status
  {:kind "Status",
   :apiVersion "v1",
   :metadata {},
   :status "Failure",
   :message "Operation cannot be fulfilled on namespaces \"carlos\": The system is ensuring all content is removed from this namespace.  Upon completion, this namespace will automatically be purged by the system.",
   :reason "Conflict",
   :details {:name "carlos", :kind "namespaces"},
   :code 409})
