(ns adapter.application
  (:require [midje.sweet :refer :all]
            [soil.adapters.application :as adapters.application]
            [schema.core :as s]
            [soil.models.application :as models.application]))
(s/set-fn-validation! true)

(s/def kratos-application :- models.application/Application
  #:application {:name       "kratos"
                 :devspace   "carlos-rodrigues"
                 :containers [#:container {:name      "kratos"
                                           :image     "formicarium/chamber-lein:latest"
                                           :syncable? true
                                           :env       {"STARTUP_CLONE"   "true"
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
                 :patches    [{:kind  "Deployment"
                               :patch {:op    "add"
                                       :path  "/spec/template/metadata/annotations/iam.amazonaws.com~1role"
                                       :value "role-arn"}}]})

(def kratos-deployment
  {:apiVersion "apps/v1"
   :kind "Deployment"
   :metadata {:annotations {"formicarium.io/patches" "[{:op \"add\", :path \"/spec/template/metadata/annotations/iam.amazonaws.com~1role\", :value \"role-arn\"}]"
                            "formicarium.io/syncable-containers" "#{\"kratos\"}"}
              :labels {"formicarium.io/application" "kratos"}
              :name "kratos"
              :namespace "carlos-rodrigues"}
   :spec {:replicas 1
          :selector {:matchLabels {"formicarium.io/application" "kratos"}}
          :template {:metadata {:annotations {"iam.amazonaws.com/role" "role-arn"}
                                :labels {"formicarium.io/application" "kratos"}
                                :namespace "carlos-rodrigues"}
                     :spec {:containers [{:env [{:name "STARTUP_CLONE"
                                                 :value "true"}
                                                {:name "STINGER_PORT"
                                                 :value "24000"}
                                                {:name "APP_PATH" :value "/app"}
                                                {:name "STINGER_SCRIPTS"
                                                 :value "/scripts"}]
                                          :image "formicarium/chamber-lein:latest"
                                          :name "kratos"
                                          :ports [{:containerPort 8080
                                                   :name "default"}
                                                  {:containerPort 35000
                                                   :name "repl"}]}]
                            :hostname "kratos"
                            :imagePullSecrets [{:name "docker-registry-secret"}]}}}})


(fact "externalize application to deployment"
      (adapters.application/application->deployment kratos-application ["docker-registry-secret"]) => kratos-deployment)

(def kratos-service
  {:apiVersion "v1"
   :kind "Service"
   :metadata {:annotations {"formicarium.io/patches" "[]"
                            "formicarium.io/port-types" "{\"default\" :interface.type/http, \"repl\" :interface.type/tcp}"}
              :labels {"formicarium.io/application" "kratos"}
              :name "kratos"
              :namespace "carlos-rodrigues"}
   :spec {:ports [{:name "default" :port 80 :protocol "TCP" :targetPort "default"}
                  {:name "repl" :port 35000 :protocol "TCP" :targetPort "repl"}]
          :selector {"formicarium.io/application" "kratos"}
          :type "NodePort"}})


(fact "externalize application to service"
  (adapters.application/application->service kratos-application) => kratos-service)

(def kratos-ingress
  {:apiVersion "extensions/v1beta1"
   :kind "Ingress"
   :metadata {:annotations {"formicarium.io/patches" "[]"
                            "kubernetes.io/ingress.class" "nginx"}
              :labels {"formicarium.io/application" "kratos"}
              :name "kratos"
              :namespace "carlos-rodrigues"}
   :spec {:rules [{:host "kratos.carlos-rodrigues.formicarium.host"
                   :http {:paths [{:backend {:serviceName "kratos"
                                             :servicePort "default"}
                                   :path "/"}]}}]}})

(fact "externalize application to ingress"
  (adapters.application/application->ingress kratos-application) => kratos-ingress)

