(ns adapter.application
  (:require [midje.sweet :refer :all]
            [soil.adapters.application :as adapters.application]
            [schema.core :as s]
            [soil.models.application :as models.application]))
(s/set-fn-validation! true)

(s/def kratos-application :- models.application/Application
  #:application {:name       "kratos"
                 :devspace   "carlos-rodrigues"
                 :status     :application.status/template
                 :containers [#:container {:name  "kratos"
                                           :image "formicarium/chamber-lein:latest"
                                           :env   {"STARTUP_CLONE"   "true"
                                                   "STINGER_PORT"    "24000"
                                                   "APP_PATH"        "/app"
                                                   "STINGER_SCRIPTS" "/scripts"}}]
                 :interfaces [#:interface {:name      "default"
                                           :port      8080
                                           :type      :interface.type/http
                                           :container "kratos"
                                           :host      "kratos.carlos-rodrigues.formicarium.host"}
                              #:interface {:name      "repl"
                                           :port      35000
                                           :type      :interface.type/tcp
                                           :container "kratos"
                                           :host      "kratos-repl.carlos-rodrigues.formicarium.host"}]
                 :syncable?  true})

(def kratos-deployment
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:name      "kratos"
                :labels    {:app "kratos"}
                :namespace "carlos-rodrigues"}
   :spec       {:selector {:matchLabels {:app "kratos"}}
                :replicas 1
                :template {:metadata {:labels    {:app "kratos"}}
                           :spec     {:containers [{:name  "kratos"
                                                    :image "formicarium/chamber-lein:latest"
                                                    :ports [{:name          "default"
                                                             :containerPort 8080}
                                                            {:name          "repl"
                                                             :containerPort 35000}]
                                                    :env   [{:name  "STARTUP_CLONE"
                                                             :value "true"}
                                                            {:name  "STINGER_PORT"
                                                             :value "24000"}
                                                            {:name  "APP_PATH"
                                                             :value "/app"}
                                                            {:name  "STINGER_SCRIPTS"
                                                             :value "/scripts"}]}]}}}})


(fact "externalize application to deployment"
      (adapters.application/application->deployment kratos-application) => kratos-deployment)

(def kratos-service
  {:apiVersion "v1"
   :kind       "Service"
   :metadata   {:name      "kratos"
                :labels    {:app "kratos"}
                :namespace "carlos-rodrigues"}
   :spec       {:ports    [{:protocol   "TCP"
                            :name       "default"
                            :port       80
                            :targetPort "default"}
                           {:protocol   "TCP"
                            :name       "repl"
                            :port       35000
                            :targetPort "repl"}]
                :selector {:app "kratos"}}})


(fact "externalize application to service"
      (adapters.application/application->service kratos-application) => kratos-service)

(def kratos-ingress
  {:apiVersion "extensions/v1beta1"
   :kind       "Ingress"
   :metadata   {:name        "kratos"
                :annotations {"kubernetes.io/ingress.class" "nginx"}
                :labels      {:app "kratos"}
                :namespace   "carlos-rodrigues"}
   :spec       {:rules [{:host "kratos.carlos-rodrigues.formicarium.host"
                         :http {:paths [{:backend {:serviceName "kratos"
                                                   :servicePort "default"}
                                         :path    "/"}]}}]}})

(fact "externalize application to ingress"
      (adapters.application/application->ingress kratos-application) => kratos-ingress)

(def kratos-config-map
  {:data {:4053 "carlos-rodrigues/kratos:35000"}})

(fact "externalize application to config-map"
      (adapters.application/application->config-map [4053] kratos-application) => kratos-config-map
      (adapters.application/application->config-map [] kratos-application) => throws
      (adapters.application/application->config-map [4053 4054 5042] kratos-application) => kratos-config-map)

