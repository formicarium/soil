(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.controllers.services :as c-svc]
            [soil.logic.devspace :as l-env]
            [soil.logic.services :as l-svc]
            [soil.diplomat.kubernetes :as d-k8s]
            [soil.config :as config]))

(defn create-devspace
  [devspace config k8s-client]
  (let [namespace (:name devspace)]
    (merge {:namespace (-> (p-k8s/create-namespace k8s-client namespace {:kind config/fmc-devspace-label})
                           l-env/namespace->devspace)}
           (c-svc/create-kubernetes-resources! (l-svc/hive->kubernetes namespace config)
                                               k8s-client)
           (c-svc/create-kubernetes-resources! (l-svc/tanajura->kubernetes namespace config)
                                               k8s-client))))

(def hive-host-template "http://hive.{{devspace}}.formicarium.host")
(def tanajura-host-template "http://tanajura.{{devspace}}.formicarium.host")
(def tanajura-git-host-template "http://git.{{devspace}}.formicarium.host")

(defn hive-host [devspace]
  (clojure.string/replace hive-host-template #"\{\{(.*)\}\}" devspace))

(defn tanajura-host [devspace]
  (clojure.string/replace tanajura-host-template #"\{\{(.*)\}\}" devspace))

(defn tanajura-git-host [devspace]
  (clojure.string/replace tanajura-git-host-template #"\{\{(.*)\}\}" devspace))

(defn list-devspaces
  [k8s-client]
  (let [devspaces (->> (p-k8s/list-namespaces k8s-client)
                       l-env/namespaces->devspaces)
        devspaces-names (map :name devspaces)]
    (->> devspaces
         (reduce (fn [acc {:keys [name]}] (conj acc name)) [])
         (map (juxt hive-host tanajura-host tanajura-git-host))
         (map #(zipmap [:hiveUrl :tanajuraApiUrl :tanajuraGitUrl] %))
         (map (fn [devspace url] {devspace url}) devspaces-names)
         (reduce merge))))

(defn delete-devspace
  [devspace k8s-client]
  (do (p-k8s/delete-namespace k8s-client (:name devspace))
      {:success true}))

