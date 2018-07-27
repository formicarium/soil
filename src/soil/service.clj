(ns soil.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [io.pedestal.interceptor :as int]
            [cheshire.core :as cheshire]
            [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.controllers.devspaces :as c-env]
            [soil.controllers.services :as c-svc]
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
   :body    {:devspaces (c-env/list-devspaces (get-in request [:components :k8s-client]))}})

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
  [request]
  {:status  200
   :headers {}
   :body    (c-env/create-devspace (:json-params request) (get-in request [:components :k8s-client]))})

(defn delete-devspace
  [request]
  {:status  200
   :headers {}
   :body    (c-env/delete-devspace (:json-params request) (get-in request [:components :k8s-client]))})

(defn deploy-service
  [request]
  {:status  200
   :headers {}
   :body    (c-svc/deploy-service! (:json-params request)
                                   (or (get-in request [:headers "formicarium-devspace"]) "default")
                                   (get-in request [:components :k8s-client])
                                   (get-in request [:components :configserver]))})

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
              ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
              ::http/type            :jetty
              ::http/port            8080})

(defn create-service
  [env]
  (case env
    :prod (merge service {:env :prod})
    :dev (merge service {:env :dev})
    :test (merge service {:env :test})))

