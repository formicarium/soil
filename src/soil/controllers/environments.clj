(ns soil.controllers.environments
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.logic.environment :as l-env]))

(defn create-environment
  [environment k8s-client]
  (-> (p-k8s/create-namespace k8s-client (:name environment))
      l-env/namespace->environment))

(defn list-environments
  [k8s-client]
  (->> (p-k8s/list-namespaces k8s-client)
       (map l-env/namespace->environment)))

(defn delete-environment
  [environment k8s-client]
  (do (p-k8s/delete-namespace k8s-client (:name environment))
      {:success true}))

