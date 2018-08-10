(ns soil.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [cheshire.core :as cheshire]
            [soil.controllers.devspaces :as c-env]
            [soil.controllers.services :as c-svc]
            [soil.components.api.soil-api :as soil-api]
            [io.pedestal.interceptor.helpers :as int-helpers]))

(def externalize-json (int-helpers/on-response ::json-response
                                               (fn [response]
                                                 (-> response
                                                     (update-in [:body] (fn [body] (if body
                                                                                     (cheshire/generate-string body)
                                                                                     body)))
                                                     (update-in [:headers] (fn [headers] (-> (or headers {})
                                                                                             (assoc "Content-Type" "application/json"))))))))

(defn get-devspaces
  [request]
  {:status  200
   :headers {}
   :body    (c-env/list-devspaces (get-in request [:components :k8s-client]))})

(defn get-health
  [request]
  {:status  200
   :headers {}
   :body    {:healthy true}})

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
    body :json-params
    headers :headers}]
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
  `[[["/" ^:interceptors [(body-params/body-params) externalize-json]
      ["/api"
       ["/health" {:get [:get-health get-health]}]
       ["/devspaces"
        {:get get-devspaces}
        {:post create-devspace}
        {:delete delete-devspace}]
       ["/services"
        {:post [:deploy-service deploy-service]}
        {:delete [:destroy-service destroy-service]}]]]]])


;; Consumed by soil.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env                   :prod
              ::http/routes          routes
              ::http/allowed-origins {:creds           true
                                      :allowed-origins (constantly true)}
              ::http/type            :jetty
              ::http/join?           true
              ::http/port            8080})

(defn create-service
  [env]
  (case env
    :prod (merge service {:env :prod})
    :dev (merge service {:env :dev})
    :test (merge service {:env :test})))

