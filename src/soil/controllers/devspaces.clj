(ns soil.controllers.devspaces
  (:require [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.controllers.services :as controllers.services]
            [soil.logic.devspace :as logic.devspace]
            [soil.logic.services :as logic.service]
            [soil.adapters.application :as adapters.application]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [clj-service.protocols.config :as protocols.config]
            [schema.core :as s]
            [selmer.parser]
            [soil.models.application :as models.application]
            [clojure.java.io :as io]))

(s/defn ^:private load-application-template :- models.application/Application
  [name :- s/Str
   replace-map :- (s/pred map?)
   config :- protocols.config/IConfig]
  (-> (str "templates/" name ".edn")
      io/resource
      slurp
      (selmer.parser/render replace-map)
      read-string
      (adapters.application/definition->application config)))

(s/defn hive-application :- models.application/Application
  [devspace :- s/Str
   config :- protocols.config/Config]
  (load-application-template "hive" {:devspace devspace} config))

(s/defn tanajura-application :- models.application/Application
  [devspace :- s/Str
   config :- protocols.config/Config]
  (load-application-template "tanajura" {:devspace devspace} config))

(s/defn create-devspace!
  [devspace-name :- s/Str
   config :- protocols.config/IConfig
   k8s-client :- protocols.k8s/IKubernetesClient]
  (diplomat.kubernetes/create-namespace! devspace-name k8s-client)
  (merge
    (controllers.services/create-kubernetes-resources! (logic.service/hive->kubernetes devspace-name config)
      k8s-client)
    (controllers.services/create-kubernetes-resources! (logic.service/tanajura->kubernetes devspace-name config)
      k8s-client)))

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
        devspaces       (->> (protocols.k8s/list-namespaces k8s-client)
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
  (do (protocols.k8s/delete-namespace! k8s-client devspace)
      {:success true}))

