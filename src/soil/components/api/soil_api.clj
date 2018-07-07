(ns soil.components.api.soil-api
  (:require [com.stuartsierra.component :as component]
            [soil.protocols.config.config :as p-cfg]
            [schema.core :as s]
            [cheshire.core :as cheshire]
            [beamly-core.config :as cfg]
            [io.pedestal.http :as server]
            [io.pedestal.http :as http]
            [soil.service :as service]
            [io.pedestal.interceptor :as int]
            [io.pedestal.interceptor.helpers :as int-helpers]))

(defn context-fn-json
  [req-resp context]
  (update-in context [req-resp :body] (fn [body] (if body
                                                   (cheshire/parse-string body true)
                                                   body))))

(def on-resp (int-helpers/on-response ::json-response
                                      (fn [response]
                                        (update-in response [:body] (fn [body] (if body
                                                                                 (cheshire/parse-string body true)
                                                                                 body))))))


(def json-body
  (int/interceptor {:name  ::json-body
                    :leave (partial context-fn-json :response)}))

(defn inject-components-on-request
  [components-map]
  {:name ::inject-components-on-request
   :enter (fn [context]
            (assoc-in context [:request :components] components-map))})


(defn create-interceptor
  [components-map]
  (int-helpers/on-request ::inject-components
                          (fn [request]
                            (assoc request :components components-map))))

(defn add-interceptor
  [service interceptor]
  (update-in service [::http/interceptors] #(vec (cons interceptor %))))



(defrecord SoilApi [k8s-client configserver]
  component/Lifecycle
  (start [this]
    (-> service/service
        server/default-interceptors
        server/dev-interceptors
        (add-interceptor (create-interceptor this))
        server/create-server
        server/start))
  (stop [this] this))

(defn new-soil-api []
  (map->SoilApi {}))
