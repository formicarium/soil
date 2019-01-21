(ns adapter.application
  (:require [midje.sweet :refer :all]
            [soil.adapters.application :as adapters.application]
            [schema.core :as s]
            [soil.models.application :as models.application]))
(s/set-fn-validation! true)

(s/def kratos-application :- models.application/Application
  #:application {:name       "kratos"
                 :service    "kratos"
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
                                       :value "role-arn"}}
                              {:kind  "Deployment"
                               :patch {:op    "add"
                                       :path  "/spec/template/spec/volumes"
                                       :value [{:name     "shared-m2"
                                                :hostPath {:path "/var/.m2"
                                                           :type "DirectoryOrCreate"}}]}}
                              {:kind  "Deployment"
                               :patch {:op    "add"
                                       :path  "/spec/template/spec/containers/0/volumeMounts"
                                       :value [{:name      "shared-m2"
                                                :mountPath "/root/.m2"}]}}]})

(def kratos-deployment
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:annotations {"formicarium.io/patches"             "[{:op \"add\", :path \"/spec/template/metadata/annotations/iam.amazonaws.com~1role\", :value \"role-arn\"} {:op \"add\", :path \"/spec/template/spec/volumes\", :value [{:name \"shared-m2\", :hostPath {:path \"/var/.m2\", :type \"DirectoryOrCreate\"}}]} {:op \"add\", :path \"/spec/template/spec/containers/0/volumeMounts\", :value [{:name \"shared-m2\", :mountPath \"/root/.m2\"}]}]"
                              "formicarium.io/syncable-containers" "#{\"kratos\"}"
                              "formicarium.io/args"                "{}"}
                :labels      {"formicarium.io/application" "kratos"
                              "formicarium.io/service"     "kratos"}
                :name        "kratos"
                :namespace   "carlos-rodrigues"}
   :spec       {:replicas 1
                :selector {:matchLabels {"formicarium.io/application" "kratos"}}
                :template {:metadata {:annotations {"iam.amazonaws.com/role" "role-arn"}
                                      :labels      {"formicarium.io/application" "kratos"}
                                      :namespace   "carlos-rodrigues"}
                           :spec     {:volumes          [{:name     "shared-m2"
                                                          :hostPath {:path "/var/.m2"
                                                                     :type "DirectoryOrCreate"}}]
                                      :containers       [{:env          [{:name  "STARTUP_CLONE"
                                                                          :value "true"}
                                                                         {:name  "STINGER_PORT"
                                                                          :value "24000"}
                                                                         {:name "APP_PATH" :value "/app"}
                                                                         {:name  "STINGER_SCRIPTS"
                                                                          :value "/scripts"}]
                                                          :image        "formicarium/chamber-lein:latest"
                                                          :name         "kratos"
                                                          :volumeMounts [{:name      "shared-m2"
                                                                          :mountPath "/root/.m2"}]
                                                          :ports        [{:containerPort 8080
                                                                          :name          "default"}
                                                                         {:containerPort 35000
                                                                          :name          "repl"}]}]
                                      :hostname         "kratos"
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
   :spec {:ports [{:name "default" :port 8080 :protocol "TCP" :targetPort "default"}
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

(fact "application->urls"
  (adapters.application/application->urls #:application{:name       "hive"
                                                        :service    "hive"
                                                        :devspace   "carlos"
                                                        :containers [#:container{:name      "hive"
                                                                                 :image     "formicarium/hive:5d98c11ea5db32bd8db5478e7389f0ac668d79b3"
                                                                                 :env       {}
                                                                                 :syncable? false}]
                                                        :interfaces [#:interface{:name      "default"
                                                                                 :port      8080
                                                                                 :type      :interface.type/http
                                                                                 :container "hive"
                                                                                 :host      "hive.carlos.formicarium.host"}
                                                                     #:interface{:name      "repl"
                                                                                 :port      2222
                                                                                 :type      :interface.type/nrepl
                                                                                 :container "hive"
                                                                                 :host      "10.129.218.235:30292"}
                                                                     #:interface{:name      "zmq"
                                                                                 :port      9898
                                                                                 :type      :interface.type/tcp
                                                                                 :container "hive"
                                                                                 :host      "10.129.218.235:32372"}]
                                                        :patches    nil}) => {:default "http://hive.carlos.formicarium.host"
                                                                              :repl  "nrepl://10.129.218.235:30292"
                                                                              :zmq   "tcp://10.129.218.235:32372"})
