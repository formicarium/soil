(ns soil.components.config-server-client
  (:require [soil.protocols.config-server-client :as protocols.config-server]
            [clj-service.protocols.config :as protocols.config]
            [schema.core :as s]
            [org.httpkit.client :as http-client]
            [cheshire.core :as cheshire]
            [com.stuartsierra.component :as component]))

(def ServiceConfiguration
  {(s/optional-key :environment-variables) {s/Keyword s/Str}
   (s/optional-key :image)                 s/Str
   :name                                   s/Str
   (s/optional-key :git)                   s/Str
   (s/optional-key :git-branch)            s/Str
   (s/optional-key :build-tool)            s/Str
   (s/optional-key :ports)                 [s/Int]})

(def ServiceArgs
  {s/Keyword s/Any})

(defn http-post
  [url body]
  @(http-client/post url
     {:body    (cheshire/generate-string body)
      :headers {"Content-Type" "application/json"}}))

(defn str->json [str]
  (cheshire/parse-string str true))

(s/defn on-deploy-service :- ServiceConfiguration
  [service-args :- ServiceArgs
   config-server]
  (-> (http-post (str (:url config-server) "/ondeployservice") service-args)
      :body
      str->json))

(s/defn on-new-devspace :- {s/Keyword ServiceConfiguration}
  [devspace config-server]
  (-> (http-post (str (:url config-server) "/onnewdevspace") devspace)
      :body
      str->json))

(defrecord ConfigServer [config]
  protocols.config-server/ConfigServerClient
  (on-new-devspace [this devspace] (on-new-devspace devspace this))
  (on-deploy-service [this service-args] (on-deploy-service service-args this))

  component/Lifecycle
  (start [this] (assoc this :url (protocols.config/get-in! config [:config-server :url])))
  (stop [this] (dissoc this :url)))

(defn new-config-server
  []
  (map->ConfigServer {}))
