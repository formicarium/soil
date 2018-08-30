(ns soil.components
  (:require [com.stuartsierra.component :as component]
            [soil.routes :as routes]
            [clj-service.components.pedestal :as components.pedestal]
            [clj-service.components.config :as components.config]
            [clj-service.components.webapp :as components.webapp]
            [soil.components.kubernetes-client :as components.kubernetes-client]
            [soil.components.etcd :as components.etcd]
            [soil.components.config-server-client :as components.config-server-client]))

(defn base [env]
  {:config         (components.config/new-config (str (name env) ".edn"))
   :config-server  (component/using (components.config-server-client/new-config-server) [:config])
   :k8s-client     (component/using (components.kubernetes-client/new-k8s-client) [:config])
   :etcd           (component/using (components.etcd/new-etcd) [:config])
   :webapp         (component/using (components.webapp/new-webapp) [:config :config-server :k8s-client :etcd])
   :pedestal       (component/using (components.pedestal/new-pedestal #'routes/routes) [:config :webapp])})

(defn test-system [env]
  (merge
    (base env)
    {:k8s-api-server {}}))

(defn system-map
  [env]
  (case env
    :test (test-system env)
    (base env)))

