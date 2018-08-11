(ns soil.adapters.application
  (:require [schema.core :as s]
            [soil.schemas.application :as schemas.application]
            [soil.models.application :as models.application]
            [clj-service.protocols.config :as protocols.config]
            [soil.logic.interface :as logic.interface]))

(s/defn definition->application :- models.application/Application
  [app-definition :- schemas.application/ApplicationDefinition
   config :- protocols.config/IConfig]
  (let [domain (protocols.config/get-in! config [:formicarium :domain])]
    #:application{:name       (:name app-definition)
                  :devspace   (:devspace app-definition)
                  :containers (mapv #(do #:container{:name  (:name %)
                                                     :image (:image %)
                                                     :env   (:env %)}) (:containers app-definition))
                  :interfaces (mapv #(logic.interface/new
                                       (merge %
                                         {:devspace (:devspace app-definition)
                                          :service  (:name app-definition)
                                          :type     (keyword "interface.type" (name (:type %)))
                                          :domain   domain})) (:interfaces app-definition))
                  :syncable?  (:syncable? app-definition)
                  :status     :application.status/template}))

(s/defn application+container->ports :- [(s/pred map?)]
  [{:application/keys [interfaces]} :- models.application/Application
   container :- models.application/Container]
  (->> interfaces
       (filter #(= (:container/name container) (:interface/container %)))
       (mapv #(do {:name          (:interface/name %)
                   :containerPort (:interface/port %)}))))

(s/defn application->containers :- [(s/pred map?)]
  [{:application/keys [containers] :as application} :- models.application/Application]
  (mapv (fn [container]
          {:name  (:container/name container)
           :image (:container/image container)
           :ports (application+container->ports application container)
           :env   (mapv #(do {:name  (clojure.core/name (key %))
                              :value (str (val %))}) (:container/env container))}) containers))

(s/defn application->deployment :- (s/pred map?)
  [{:application/keys [devspace] :as application} :- models.application/Application]
  (let [app-name (:application/name application)]
    {:apiVersion "apps/v1"
     :kind       "Deployment"
     :metadata   {:name      app-name
                  :labels    {:app app-name}
                  :namespace devspace}
     :spec       {:selector {:matchLabels {:app app-name}}
                  :replicas 1
                  :template {:metadata {:labels {:app app-name}}
                             :spec     {:containers (application->containers application)}}}}))

(s/defn application->services :- [(s/pred map?)]            ;; TODO: Write correctly
  [application :- models.application/Application]
  (let [app-name (:application/name application)]
    {:apiVersion "v1"
     :kind       "Service"
     :metadata   {:name      app-name
                  :labels    {:app app-name}
                  :namespace (:application/devspace application)}
     :spec       {:ports    [{:protocol   "TCP"
                              :name       "http"
                              :port       80
                              :targetPort "http"}]
                  :selector {:app app-name}}}))

(s/defn application->ingress :- (s/pred map?)               ;; TODO: Write correctly
  [application :- models.application/Application]
  (let [app-name (:application/name application)]
    {:apiVersion "extensions/v1beta1"
     :kind       "Ingress"
     :metadata   {:name        app-name
                  :annotations {"kubernetes.io/ingress.class" "nginx"}
                  :labels      {:app app-name}
                  :namespace   (:application/devspace application)}
     :spec       {:rules [{:host "kratos-other.carlos-rodrigues.domain.host"
                           :http {:paths [{:backend {:serviceName "kratos-other"
                                                     :servicePort "kratos-other"}
                                           :path    "/"}]}}
                          {:host "kratos.carlos-rodrigues.domain.host"
                           :http {:paths [{:backend {:serviceName "kratos-api"
                                                     :servicePort "kratos-api"}
                                           :path    "/"}]}}]}}))
