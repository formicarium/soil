(ns spin-env
  (:require [selvage.flow :refer [flow *world*]]
            [midje.sweet :refer :all]
            [clj-http.client :as http-client]
            [soil.component :as soil]
            [com.stuartsierra.component :as component]
            [cheshire.core :as cheshire]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :refer [response-for]]
            [soil.service :as service]
            [soil.components.api.soil-api :as soil-api])
  (:use org.httpkit.fake))

(defn expose-service
  [world]
  (assoc world :service-fn (get-in world [:system :soil-api :service ::http/service-fn])))

(defn init!
  [world]
  (-> world
      (assoc :system (component/start-system (soil/system-map :test)))
      expose-service))


(defn json->str [coll] (cheshire/generate-string coll))

(defn str->json [str] (cheshire/parse-string str true))


(defn create-env-req! [service] (response-for service :post "/api/environments"
                                              :headers {"Content-Type" "application/json"}
                                              :body    (json->str {:name "carlos"})))

#_(defn create-env!
  [world]
  (assoc world :env-created (with-fake-routes-in-isolation
                              {"http://google.com" {:post (fn [req]
                                                                                  {:status 200
                                                                                   :header {}
                                                                                   :body   (json->str {:apiVersion "v1"
                                                                                                       :kind       "Namespace"
                                                                                                       :metadata   {:name "carlos"}})})}}
                              (create-env-req! (:service-fn world)))))


(defn create-env!
  [world]
  (assoc world :env-created (with-fake-http [{:url    "http://localhost:9000/api/v1/namespaces"
                                              :method :post} {:body (json->str {:apiVersion "v1"
                                                                                :kind       "Namespace"
                                                                                :metadata   {:name "carlos"}})}]
                              (create-env-req! (:service-fn world)))))

(flow "spin up a new environment"
      init!
      (fn [world] (let [service (:service-fn world)] (assoc world :service-health (response-for service :get "/api/health"))))
      (fact "health must answer 200"
            (:service-health *world*) => (contains {:status 200
                                                    :body   (json->str {:healthy true})}))
      create-env!
      (fact "environment 'carlos' must have been created"
            (:env-created *world*) => (contains {:status 200
                                                 :body   (json->str {:name "carlos"})})))
