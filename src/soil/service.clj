(ns soil.service
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
            [soil.config :as config]))

(defn get-health [_]
  {:status 200
   :body   {:healthy true}})

(defn get-version
  [{{:keys [config]} :components}]
  {:status 200
   :body   {:version (config/version config)}})

(defn get-devspaces
  [{{:keys [config k8s-client]} :components}]
  {:status 200
   :body   (controllers.devspace/list-devspaces k8s-client config)})

(defn create-devspace
  [{{:keys [config k8s-client]} :components
    {devspace-name :name}       :data}]
  {:status 200
   :body   (controllers.devspace/create-devspace! devspace-name config k8s-client)})

(defn delete-devspace
  [{{:keys [k8s-client]} :components
    devspace             :devspace-name}]
  {:status 200
   :body   (controllers.devspace/delete-devspace devspace k8s-client)})

(defn deploy-service
  [{{:keys [k8s-client config-server config]} :components
    body                                      :json-params
    devspace-name                             :devspace-name}]
  {:status 200
   :body   (controllers.service/deploy-service! body devspace-name k8s-client config-server config)})

(defn delete-service
  [{{:keys [k8s-client]} :components
    devspace-name        :devspace-name
    service-name         :service-name}]
  {:status 200
   :body   (controllers.service/destroy-service! service-name devspace-name k8s-client)})

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
          {:get  [:get-devspaces
                  ^:interceptors [(int-schema/coerce schemas.devspace/CreateDevspace)]
                  get-devspaces]
           :post [:create-devspace create-devspace]}

          ["/:devspace-name" ^:interceptors [(int-adapt/coerce-path :devspace-name s/Str)]
           {:delete [:delete-devspaces delete-devspace]}

           ["/services"
            {:post [:deploy-service
                    ^:interceptors [(int-schema/coerce schemas.service/DeployService)]
                    deploy-service]}

            ["/:service-name" ^:interceptors [(int-adapt/coerce-path :service-name s/Str)]
             {:delete [:delete-service delete-service]}]]]]]]]]))

