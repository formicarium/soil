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
                                                 (println response)
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
  (println (keys request))
  (println (keys (:components request)))
  {:status 200
   :headers {}
   :body {:healthy true}})

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
  (println request)
  {:status  200
   :headers {}
   :body    (c-svc/deploy-service! (:json-params request)
                                   (or (get-in request [:headers "formicarium-devspace"]) "default")
                                   (get-in request [:components :k8s-client])
                                   (get-in request [:components :configserver]))})

(defn destroy-service
  [request]
  {:status 200
   :headers {}
   :body (c-svc/destroy-service! (get-in request [:json-params :name])
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
(def service {:env                     :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes            routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Tune the Secure Headers
              ;; and specifically the Content Security Policy appropriate to your service/application
              ;; For more information, see: https://content-security-policy.com/
              ;;   See also: https://github.com/pedestal/pedestal/issues/499
              ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
              ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
              ;;                                                          :frame-ancestors "'none'"}}

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path     "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
              ::http/type              :jetty
              ;;::http/host "localhost"
              ::http/port              8080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2?  false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})

(defn create-service
  [env]
  (case env
    :prod (merge service {:env :prod})
    :dev  (merge service {:env :dev})
    :test (merge service {:env :test})))

