(ns soil.logic.environment)

(defn namespace->environment
  [namespace]
  {:name (get-in namespace [:metadata :name])})

(defn namespaces->environments
  [namespaces]
  (map namespaces namespace->environment))

