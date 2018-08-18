(ns soil.routes
  (:require [io.pedestal.http.body-params :as body-params]
            [soil.controllers.devspaces :as controllers.devspace]
            [soil.controllers.services :as controllers.service]
            [soil.schemas.service :as schemas.service]
            [soil.schemas.devspace :as schemas.devspace]
            [clj-service.pedestal.interceptors.adapt :as int-adapt]
            [clj-service.pedestal.interceptors.error :as int-err]
            [clj-service.pedestal.interceptors.schema :as int-schema]
            [io.pedestal.http.route :as route]
            [schema.core :as s]
            [soil.config :as config]
            [soil.adapters.application :as adapters.application]
            [soil.adapters.devspace :as adapters.devspace]))

(defn get-health [_]
  {:status 200
   :body   {:healthy true}})

(defn get-version
  [{{:keys [config]} :components}]
  {:status 200
   :body   {:version (config/version config)}})

(defn get-devspaces
  [{{:keys [etcd]} :components}]
  {:status 200
   :body   (->> (controllers.devspace/get-devspaces etcd)
                (mapv adapters.devspace/internal->wire))})

(defn one-devspace
  [{{:keys [etcd k8s-client]} :components
    devspace-name  :devspace-name}]
  {:status 200
   :body   (->> (controllers.devspace/one-devspace devspace-name etcd k8s-client)
                adapters.devspace/internal->wire)})

(defn create-devspace!
  [{{:keys [config k8s-client etcd]} :components
    {devspace-name :name}            :data}]
  {:status 201
   :body   (-> (controllers.devspace/create-devspace! devspace-name config etcd k8s-client)
               adapters.devspace/internal->wire)})

(defn delete-devspace!
  [{{:keys [k8s-client etcd]} :components
    devspace                  :devspace-name}]
  (controllers.devspace/delete-devspace! devspace etcd k8s-client)
  {:status 200
   :body   {}})

(defn create-service!
  [{{:keys [k8s-client config-server config etcd]} :components
    service-deploy                                 :json-params
    devspace-name                                  :devspace-name}]
  {:status 200
   :body   (-> (controllers.service/create-service! service-deploy devspace-name config etcd k8s-client config-server)
               adapters.application/application->urls)})

(defn delete-service!
  [{{:keys [k8s-client etcd config]} :components
    devspace-name                    :devspace-name
    service-name                     :service-name}]
  {:status 200
   :body   (controllers.service/delete-service! service-name devspace-name etcd config k8s-client)})

(defn one-service
  [{{:keys [etcd k8s-client]} :components
    devspace-name             :devspace-name
    service-name              :service-name}]
  {:status 200
   :body   (-> (controllers.service/one-service devspace-name service-name etcd k8s-client)
               adapters.application/internal->wire)})

(def routes
  (route/expand-routes
    `[[["/" ^:interceptors [int-err/catch!
                            (body-params/body-params)
                            int-adapt/coerce-body
                            int-adapt/content-neg-intc
                            int-schema/coerce-output]
        ["/api"
         ["/health" {:get [:get-health get-health]}]
         ["/version" {:get [:get-version get-version]}]

         ["/devspaces"
          {:get  [:get-devspaces get-devspaces]
           :post [:create-devspace ^:interceptors [(int-schema/coerce schemas.devspace/CreateDevspace)]
                  create-devspace!]}

          ["/:devspace-name" ^:interceptors [(int-adapt/coerce-path :devspace-name s/Str)]
           {:get    [:one-devspace one-devspace]
            :delete [:delete-devspace delete-devspace!]}
           ["/services"
            {:post [:deploy-service
                    ^:interceptors [(int-schema/coerce schemas.service/DeployService)]
                    create-service!]}

            ["/:service-name" ^:interceptors [(int-adapt/coerce-path :service-name s/Str)]
             {:get    [:one-service one-service]
              :delete [:delete-service delete-service!]}]]]]]]]]))

