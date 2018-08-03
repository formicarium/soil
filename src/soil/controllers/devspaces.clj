(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.controllers.services :as c-svc]
            [soil.logic.devspace :as l-env]
            [soil.logic.services :as l-svc]
            [soil.config :as config]))

(defn create-devspace
  [devspace config k8s-client]
  (merge {:namespace (-> (p-k8s/create-namespace k8s-client (:name devspace) {:kind config/fmc-devspace-label})
                         l-env/namespace->devspace)}
         (c-svc/create-kubernetes-resources! (l-svc/hive->kubernetes (:name devspace) config) k8s-client)))

(defn list-devspaces
  [k8s-client]
  (->> (p-k8s/list-namespaces k8s-client)
       l-env/namespaces->devspaces))

(defn delete-devspace
  [devspace k8s-client]
  (do (p-k8s/delete-namespace k8s-client (:name devspace))
      {:success true}))

