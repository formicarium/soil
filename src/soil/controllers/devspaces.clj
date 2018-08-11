(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes-client :as protocols.kubernetes-client]
            [soil.controllers.services :as controllers.services]
            [soil.logic.devspace :as logic.devspace]
            [soil.logic.services :as logic.service]
            [soil.config :as config]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [clj-service.protocols.config :as protocols.config]))

(defn create-devspace
  [devspace config k8s-client]
  (let [namespace (:name devspace)]
    (merge {:namespace (-> (protocols.kubernetes-client/create-namespace! k8s-client namespace {:kind config/fmc-devspace-label})
                           logic.devspace/namespace->devspace)}
      (controllers.services/create-kubernetes-resources! (logic.service/hive->kubernetes namespace config)
        k8s-client)
      (controllers.services/create-kubernetes-resources! (logic.service/tanajura->kubernetes namespace config)
        k8s-client))))

(defn hive-api-url [domain devspace]
  (str "http://hive." devspace "." domain))

(defn hive-repl-host [domain k8s-client devspace]
  (let [repl-port (->> k8s-client
                       diplomat.kubernetes/get-nginx-tcp-config-map
                       (logic.service/get-repl-port devspace "hive"))]
    (when repl-port
      (str "nrepl://hive." devspace "." domain ":" repl-port))))

(defn tanajura-api-url [domain devspace]
  (str "http://tanajura." devspace "." domain))

(defn tanajura-git-url [domain devspace]
  (str "http://git." devspace "." domain))

(defn config-server-url [config devspace]
  (protocols.config/get-in! config [:config-server :url]))

(defn list-devspaces
  [k8s-client config]
  (let [top-level       (protocols.config/get-in! config [:formicarium :domain])
        devspaces       (->> (protocols.kubernetes-client/list-namespaces k8s-client)
                             logic.devspace/namespaces->devspaces)
        devspaces-names (map :name devspaces)]
    (->> devspaces
         (reduce (fn [acc {:keys [name]}] (conj acc name)) [])
         (map (juxt
                (partial hive-api-url top-level)
                (partial hive-repl-host top-level k8s-client)
                (partial tanajura-api-url top-level)
                (partial tanajura-git-url top-level)
                (partial config-server-url config)))
         (map #(zipmap [:hiveApiUrl :hiveReplUrl :tanajuraApiUrl :tanajuraGitUrl :configServerUrl] %))
         (map (fn [devspace url] {devspace url}) devspaces-names)
         (reduce merge))))

(defn delete-devspace
  [devspace k8s-client]
  (do (protocols.kubernetes-client/delete-namespace! k8s-client (:name devspace))
      {:success true}))

