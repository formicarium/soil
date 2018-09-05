(ns soil.components.config-server-client
  (:require [soil.protocols.config-server-client :as protocols.config-server]
            [clj-service.protocols.config :as protocols.config]
            [schema.core :as s]
            [org.httpkit.client :as http-client]
            [com.stuartsierra.component :as component]
            [clj-service.adapt :as adapt]
            [soil.schemas.application :as schemas.application]
            [clj-service.schema :as schema]))

(def ConfigServerArgs {s/Keyword s/Any})
(def create-devspace-path "/api/devspace/create")
(def create-service-path "/api/service/create")

(s/defn http-post
  [url :- s/Str
   body :- s/Any]
  @(http-client/request {:url     url
                         :method  :post
                         :body    (adapt/to-json body)
                         :headers {"Content-Type" "application/json"}}))

(s/defn on-create-service :- [schemas.application/ApplicationDefinition]
  [service-args :- ConfigServerArgs
   config-server :- protocols.config-server/IConfigServerClient]
  (-> (http-post (str (:url config-server) create-service-path) service-args)
      :body
      adapt/from-json
      (schema/coerce [schemas.application/ApplicationDefinition])))

(s/defn on-create-devspace :- [schemas.application/ApplicationDefinition]
  [devspace-args :- ConfigServerArgs
   config-server :- protocols.config-server/IConfigServerClient]
  (-> (http-post (str (:url config-server) create-devspace-path) devspace-args)
      :body
      adapt/from-json
      (schema/coerce [schemas.application/ApplicationDefinition])))

(defrecord ConfigServer [config]
  protocols.config-server/ConfigServerClient
  (on-create-devspace [this devspace] (on-create-devspace devspace this))
  (on-create-service [this service-args] (on-create-service service-args this))

  component/Lifecycle
  (start [this] (assoc this :url (protocols.config/get-in! config [:config-server :url])))
  (stop [this] (dissoc this :url)))

(defn new-config-server [] (map->ConfigServer {}))
