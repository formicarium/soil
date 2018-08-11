(ns soil.service
  (:require [io.pedestal.http.body-params :as body-params]
            [soil.controllers.devspaces :as c-env]
            [soil.controllers.services :as c-svc]
            [clj-service.pedestal.interceptors.adapt :as int-adapt]
            [clj-service.pedestal.interceptors.error :as int-err]
            [clj-service.pedestal.interceptors.schema :as int-schema]
            [clj-service.protocols.config :as protocols.config]
            [io.pedestal.interceptor.helpers :as int-helpers]
            [io.pedestal.http.route :as route]))

(defn get-devspaces
  [{{:keys [config k8s-client]} :components}]
  {:status  200
   :headers {}
   :body    (c-env/list-devspaces k8s-client config)})

(defn get-health
  [request]
  {:status  200
   :headers {}
   :body    {:healthy true}})

(defn get-version
  [{{:keys [config]} :components}]
  {:status  200
   :headers {}
   :body    {:version (protocols.config/get-in! config [:soil :version])}})

(defn components-on-request-interceptor
  [components]
  (int-helpers/on-request ::components-on-request
    (fn [request]
      (assoc request :components components))))

(defn create-devspace
  [{{:keys [config k8s-client]} :components, json-params :json-params}]
  {:status  200
   :headers {}
   :body    (c-env/create-devspace json-params
              config
              k8s-client)})

(defn delete-devspace
  [request]
  {:status  200
   :headers {}
   :body    (c-env/delete-devspace (:json-params request) (get-in request [:components :k8s-client]))})

(defn deploy-service
  [{{:keys [k8s-client configserver config]} :components
    body                                     :json-params
    headers                                  :headers}]
  {:status  200
   :headers {}
   :body    (c-svc/deploy-service! body
              (get headers "formicarium-devspace" "default")
              k8s-client
              configserver
              config)})

(defn destroy-service
  [request]
  {:status  200
   :headers {}
   :body    (c-svc/destroy-service! (get-in request [:json-params :name])
              (or (get-in request [:headers "formicarium-devspace"]) "default")
              (get-in request [:components :k8s-client]))})

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
          {:get get-devspaces}
          {:post create-devspace}
          {:delete delete-devspace}]
         ["/services"
          {:post [:deploy-service deploy-service]}
          {:delete [:destroy-service destroy-service]}]]]]]))

