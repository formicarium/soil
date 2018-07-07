(ns soil.components.configserver.configserver-client
  (:require [soil.protocols.configserver.configserver-client :as p-cs]
            [schema.core :as s]
            [clj-http.client :as client]
            [cheshire.core :as cheshire]
            [com.stuartsierra.component :as component]
            [soil.protocols.config.config :as p-cfg]))

(def ServiceConfiguration
  {:environment-variables (s/maybe {s/Keyword s/Str})
   :image                 (s/maybe s/Str)
   :git                   (s/maybe s/Str)
   :git-branch            (s/maybe s/Str)
   :ports                 (s/maybe [s/Int])})

(def ServiceArgs
  {s/Keyword s/Str})

(defn http-post
  [body url]
  (client/post url
               {:body   (cheshire/generate-string body)
                :accept :json}))

(s/defn on-deploy-service :- ServiceConfiguration
  [service-args :- ServiceArgs
   config-server]
  (http-post (str (:url config-server) "ondeployservice") service-args))

(s/defn on-startup-environment :- {s/Keyword ServiceConfiguration}
  [env config-server]
  (http-post (str (:url config-server) "onstartupenvironment") env))

(defrecord ConfigServer [config]
  p-cs/ConfigServerClient
  (on-startup-environment [this env] (on-startup-environment env this))
  (on-deploy-service [this service-args] (on-deploy-service service-args this))

  component/Lifecycle
  (start [this] (assoc this :url (p-cfg/get-config config [:configserver :url])))
  (stop [this] (dissoc this :url)))

(defn new-configserver
  []
  (map->ConfigServer {}))
