(ns soil.logic.devspace)

(defn namespace->devspace
  [namespace]
  {:name (get-in namespace [:metadata :name])})

(defn namespaces->devspaces
  [namespaces]
  (map namespace->devspace namespaces))

