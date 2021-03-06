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
            [soil.adapters.devspace :as adapters.devspace]
            [soil.adapters.service :as adapters.service]))

(defn get-health [_]
  {:status 200
   :body   {:healthy true}})

(defn get-version
  [{{:keys [config]} :components}]
  {:status 200
   :body   {:version (config/version config)}})

(defn get-devspaces
  [{{:keys [k8s-client]} :components}]
  {:status 200
   :body   (->> (controllers.devspace/get-devspaces k8s-client)
                (mapv adapters.devspace/internal->wire))})

(defn one-devspace
  [{{:keys [k8s-client]} :components
    devspace-name        :devspace-name}]
  {:status 200
   :body   (->> (controllers.devspace/one-devspace devspace-name k8s-client)
                adapters.devspace/internal->wire)})

(defn create-devspace!
  [{{:keys [config config-server k8s-client]} :components
    new-devspace                              :data}]
  {:status 201
   :body   (-> (controllers.devspace/create-devspace! new-devspace config config-server k8s-client)
               adapters.devspace/internal->wire)})

(defn delete-devspace!
  [{{:keys [k8s-client]} :components
    devspace             :devspace-name}]
  (controllers.devspace/delete-devspace! devspace k8s-client)
  {:status 200
   :body   {}})

(defn create-service!
  [{{:keys [k8s-client config-server config]} :components
    service-deploy                            :data
    devspace-name                             :devspace-name}]
  {:status 200
   :body   (->> (controllers.service/create-service! service-deploy devspace-name config k8s-client config-server)
                adapters.service/internal->wire)})

(defn delete-service!
  [{{:keys [k8s-client config]} :components
    devspace-name               :devspace-name
    service-name                :service-name}]
  {:status 200
   :body   (controllers.service/delete-service! service-name devspace-name k8s-client)})

(defn one-service
  [{{:keys [k8s-client]} :components
    devspace-name        :devspace-name
    service-name         :service-name}]
  {:status 200
   :body   (-> (controllers.service/one-service devspace-name service-name k8s-client)
               adapters.service/internal->wire)})

(defn deploy-set!
  [{{:keys [k8s-client config-server config]} :components
    {:keys [services]}                        :data
    devspace-name                             :devspace-name}]
  {:status 200
   :body   (->> (controllers.service/deploy-service-set services devspace-name config k8s-client config-server)
                adapters.service/internal->wire)})

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

           ["/deploy-set" ^:interceptors [(int-schema/coerce schemas.service/DeploySet)]
            {:post [:deploy-set deploy-set!]}]

           ["/services"
            {:post [:deploy-service
                    ^:interceptors [(int-schema/coerce schemas.service/DeployService)]
                    create-service!]}

            ["/:service-name" ^:interceptors [(int-adapt/coerce-path :service-name s/Str)]
             {:get    [:one-service one-service]
              :delete [:delete-service delete-service!]}]]]]]]]]))

