(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.logic.devspace :as l-env]
            [soil.config :as config]))

(defn create-devspace
  [devspace k8s-client]
  (-> (p-k8s/create-namespace k8s-client (:name devspace) {:kind config/fmc-devspace-label})
      l-env/namespace->devspace))

(defn list-devspaces
  [k8s-client]
  (->> (p-k8s/list-namespaces k8s-client)
       l-env/namespaces->devspaces))

(defn delete-devspace
  [devspace k8s-client]
  (do (p-k8s/delete-namespace k8s-client (:name devspace))
      {:success true}))

