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
            [soil.components.api.soil-api :as soil-api]
            [aero.core :as aero]
            [beamly-core.config :as cfg]

            [clojure.java.io :as io])
  (:use org.httpkit.fake))
(def test-config (aero/read-config (io/resource "test.edn")))
(def kubernetes-proxy-url (get-in test-config [:kubernetes :proxy :url]))
(def configserver-url (get-in test-config [:configserver :url]))

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


(defn create-env-req! [service] (response-for service
                                              :post "/api/devspaces"
                                              :headers {"Content-Type" "application/json"}
                                              :body (json->str {:name "carlos"})))

(defn create-env!
  [world]
  (assoc world :env-created (with-fake-http [{:url    (str kubernetes-proxy-url "/api/v1/namespaces")
                                              :method :post} {:body (json->str {:apiVersion "v1"
                                                                                :kind       "Namespace"
                                                                                :metadata   {:name "carlos"}})}]
                                            (create-env-req! (:service-fn world)))))

(def deployment-example {:apiVersion "apps/v1"
                         :kind       "Deployment"
                         :metadata   {:name      "nginx"
                                      :namespace "carlos"}
                         :spec       {:replicas 1
                                      :selector {:matchLabels {:app "nginx"}}
                                      :template {:metadata {:name "nginx"
                                                            :namespace "carlos"
                                                            :labels {:app "nginx"}}
                                                 :spec     {:containers [{:name  "nginx"
                                                                          :image "nginx:1.10"
                                                                          :ports [{:containerPort 80}]}]}}}})

(def service-args {:name      "nginx"
                   :randomOps 42})

(def service-configuration {:environment-variables {:foo "foo"}
                            :name                  "nginx-svc"
                            :image                 "nginx:1.10"})


(defn create-service-req! [service]
  (response-for service :post "/api/services"
                :headers {"Content-Type"         "application/json"
                          "Formicarium-Devspace" "carlos"}
                :body (json->str service-args)))

(defn create-service!
  [world]
  (assoc world :services-deployed (with-fake-http [{:url    (str kubernetes-proxy-url "/apis/apps/v1/namespaces/carlos/deployments")
                                                    :method :post} {:status 200
                                                                    :body   (json->str deployment-example)}
                                                   {:url    (str kubernetes-proxy-url "/api/v1/namespaces/carlos/services")
                                                    :method :post} {:body (json->str {:apiVersion "v1"
                                                                                      :kind       "Service"
                                                                                      :metadata   {:name "carlos"}})}
                                                   {:url    (str kubernetes-proxy-url "/apis/extensions/v1beta1/namespaces/carlos/ingresses")
                                                    :method :post} {:body (json->str {:apiVersion "v1"
                                                                                      :kind       "Ingress"
                                                                                      :metadata   {:name "carlos"}})}
                                                   {:url    (str configserver-url "/ondeployservice")
                                                    :method :post
                                                    :body   (json->str service-args)} {:status 200
                                                                                       :body   (json->str service-configuration)}]
                                                  (create-service-req! (:service-fn world)))))

#_(flow "spin up a new devspace"
      init!
      (fn [world] (let [service (:service-fn world)]
                    (assoc world :service-health (response-for service :get "/api/health"))))
      (fact "health must answer 200"
            (:service-health *world*) => (contains {:status 200
                                                    :body   (json->str {:healthy true})}))
      create-env!
      (fact "devspace 'carlos' must have been created"
            (:env-created *world*) => (contains {:status 200
                                                 :body   (json->str {:name "carlos"})}))
      create-service!
      (fact "service 'nginx' must be deployed"
            (:services-deployed *world*) => (contains {:status 200})))
