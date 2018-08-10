(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.controllers.services :as c-svc]
            [soil.logic.devspace :as l-env]
            [soil.logic.services :as l-svc]
            [soil.config :as config]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [soil.protocols.config.config :as protocol.config]))

(defn create-devspace
  [devspace config k8s-client]
  (let [namespace (:name devspace)]
    (merge {:namespace (-> (p-k8s/create-namespace k8s-client namespace {:kind config/fmc-devspace-label})
                           l-env/namespace->devspace)}
      (c-svc/create-kubernetes-resources! (l-svc/hive->kubernetes namespace config)
        k8s-client)
      (c-svc/create-kubernetes-resources! (l-svc/tanajura->kubernetes namespace config)
        k8s-client))))

(defn hive-api-url [domain devspace]
  (str "http://hive." devspace "." domain))

(defn hive-repl-url [domain k8s-client devspace]
  (let [repl-port (->> k8s-client
                       diplomat.kubernetes/get-nginx-tcp-config-map
                       (l-svc/get-repl-port devspace "hive"))]
    (str "http://hive." devspace "." domain ":" repl-port)))

(defn tanajura-api-url [domain devspace]
  (str "http://tanajura." devspace "." domain))

(defn tanajura-git-url [domain devspace]
  (str "http://git." devspace "." domain))

(defn config-server-url [config devspace]
  (protocol.config/get-config config [:configserver :url]))

(defn list-devspaces
  [k8s-client config]
  (let [top-level       (protocol.config/get-config config [:formicarium :domain])
        devspaces       (->> (p-k8s/list-namespaces k8s-client)
                             l-env/namespaces->devspaces)
        devspaces-names (map :name devspaces)]
    (->> devspaces
         (reduce (fn [acc {:keys [name]}] (conj acc name)) [])
         (map (juxt
                (partial hive-api-url top-level)
                (partial hive-repl-url top-level k8s-client)
                (partial tanajura-api-url top-level)
                (partial tanajura-git-url top-level)
                (partial config-server-url config)))
         (map #(zipmap [:hiveApiUrl :hiveReplUrl :tanajuraApiUrl :tanajuraGitUrl :configServerUrl] %))
         (map (fn [devspace url] {devspace url}) devspaces-names)
         (reduce merge))))

(defn delete-devspace
  [devspace k8s-client]
  (do (p-k8s/delete-namespace k8s-client (:name devspace))
      {:success true}))

