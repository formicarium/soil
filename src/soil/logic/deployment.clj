(ns soil.logic.deployment
  (:require [schema.core :as s]
            [soil.protocols.configserver.configserver-client :as p-cs]))

(s/defn config->deployment
  [service-configuration namespace]
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:name      (:name service-configuration)
                :namespace namespace}
   :spec       {:selector {:matchLabels {:app (:name service-configuration)}}
                :replicas (or (:replicas service-configuration) 1)
                :template {:metadata {:labels {:app (:name service-configuration)}}
                           :spec     {:containers [{:name  (:name service-configuration)
                                                    :image (str "joker-" (:build-tool service-configuration))
                                                    :ports (map #({:containerPort %}) (:ports service-configuration))}]}}}})

(s/defn get-service-deployment
  [service-args config-server requester]
  (-> (p-cs/on-deploy-service config-server service-args)
      (config->deployment (:namespace requester))))
