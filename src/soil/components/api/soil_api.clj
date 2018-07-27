(ns soil.components.api.soil-api
  (:require [com.stuartsierra.component :as component]
            [cheshire.core :as cheshire]
            [io.pedestal.http :as server]
            [io.pedestal.http :as http]
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
  {:name  ::inject-components-on-request
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

(defn env=
  [service-map env]
  (= env (:env service-map)))

(defn env-test?
  [service-map]
  (env= service-map :test))

(defn env-dev?
  [service-map]
  (env= service-map :dev))

(defrecord SoilApi [service-map service]
  component/Lifecycle
  (start [this]
    (if service
      this
      (cond-> service-map
              true server/default-interceptors
              (env-dev? service-map) server/dev-interceptors
              true (add-interceptor (create-interceptor this))
              true server/create-server
              (not (env-test? service-map)) server/start
              true ((partial assoc this :service)))))
  (stop [this]
    (when (and service (not (env-test? service-map)))
      (server/stop service))
    (assoc this :service :nil)))

(defn new-soil-api []
  (map->SoilApi {}))
