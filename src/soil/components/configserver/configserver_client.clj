(ns soil.components.configserver.configserver-client
  (:require [soil.protocols.configserver.configserver-client :as p-cs]
            [schema.core :as s]
            [org.httpkit.client :as http-client]
            [cheshire.core :as cheshire]
            [com.stuartsierra.component :as component]
            [soil.protocols.config.config :as p-cfg]))

(def ServiceConfiguration
  {(s/optional-key :environment-variables) (s/maybe {s/Keyword s/Str})
   (s/optional-key :image)                 (s/maybe s/Str)
   (s/optional-key :git)                   (s/maybe s/Str)
   (s/optional-key :git-branch)            (s/maybe s/Str)
   (s/optional-key :build-tool)            (s/maybe s/Str)
   (s/optional-key :ports)                 (s/maybe [s/Int])})

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
  p-cs/ConfigServerClient
  (on-new-devspace [this devspace] (on-new-devspace devspace this))
  (on-deploy-service [this service-args] (on-deploy-service service-args this))

  component/Lifecycle
  (start [this] (assoc this :url "https://config-server.formicarium.host"))
  (stop [this] (dissoc this :url)))

(defn new-configserver
  []
  (map->ConfigServer {}))
