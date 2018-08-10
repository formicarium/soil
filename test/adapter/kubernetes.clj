(ns adapter.kubernetes
  (:require [midje.sweet :refer :all]
            [soil.adapters.kubernetes :as adapter.k8s]))

(def external-deployment
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:name      "kratos"
                :labels    {:app "kratos"}
                :namespace "carlos-rodrigues"}
   :spec       {:selector {:matchLabels {:app "kratos"}}
                :replicas 1
                :template {:metadata {:name      "kratos"
                                      :namespace "carlos-rodrigues"
                                      :labels    {:app "kratos"}}
                           :spec     {:containers [{:name  "kratos"
                                                    :image "formicarium/chamber-lein:latest"
                                                    :ports [{:name          "stinger"
                                                             :containerPort 24000}
                                                            {:name          "http"
                                                             :containerPort 8041}]
                                                    :env   [{:name  "STARTUP_CLONE"
                                                             :value "true"}
                                                            {:name  "STINGER_PORT"
                                                             :value "24000"}
                                                            {:name  "APP_PATH"
                                                             :value "/app"}
                                                            {:name  "STINGER_SCRIPTS"
                                                             :value "/scripts"}]}]}}}})

(def internal-deployment
  {:deployment/containers [{:container/env   [{:name "STARTUP_CLONE" :value "true"}
                                              {:name "STINGER_PORT" :value "24000"}
                                              {:name "APP_PATH" :value "/app"}
                                              {:name "STINGER_SCRIPTS" :value "/scripts"}]
                            :container/image "formicarium/chamber-lein:latest"
                            :container/name  "kratos"
                            :container/ports [{:port/container-port 24000
                                               :port/name           "stinger"}
                                              {:port/container-port 8041
                                               :port/name           "http"}]}]
   :deployment/name       "kratos"
   :deployment/namespace  "carlos-rodrigues"
   :deployment/replicas   1})

(fact "externalize deployment"
      (adapter.k8s/internalize-deployment external-deployment) => internal-deployment)

(fact "internalize deployment"
      (adapter.k8s/externalize-deployment internal-deployment) => external-deployment)

(def external-service
  {:apiVersion "v1"
   :kind       "Service"
   :metadata   {:name      "kratos"
                :labels    {:app "kratos"}
                :namespace "carlos-rodrigues"}
   :spec       {:ports    [{:protocol   "TCP"
                            :name       "http"
                            :port       80
                            :targetPort "http"}]
                :selector {:app "kratos"}}})
