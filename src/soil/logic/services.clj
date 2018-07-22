(ns soil.logic.services
  (:require [schema.core :as s]))

(s/defn config->deployment
  [service-configuration namespace]
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:name      (:name service-configuration)
                :namespace namespace}
   :spec       {:selector {:matchLabels {:app (:name service-configuration)}}
                :replicas (or (:replicas service-configuration) 1)
                :template {:metadata {:labels {:app (:name service-configuration)}}
                           :spec     {:containers [{:name         (:name service-configuration)
                                                    :image        (or (:image service-configuration)
                                                                      (str "formicarium/joker-" (:build-tool service-configuration) ":0.0.10"))
                                                    :ports        (into [{:name          "syncthing-api"
                                                                          :containerPort 24000}
                                                                         {:name          "syncthing-file"
                                                                          :containerPort 22000}] (map (fn [port] {:name          (str "svc-port-" port)
                                                                                                                  :containerPort port}) (:ports service-configuration)))
                                                    :env          (map (fn [[k v]] {:name k :value v}) (:environment-variables service-configuration))
                                                    :volumeMounts [{:name      "git-creds"
                                                                    :mountPath "/mnt/git-credentials"}]}]
                                      :volumes    [{:name   "git-creds"
                                                    :secret {:secretName "git-credentials"}}]}}}})

(defn build-response
  [k8s-resp]
  (if (= (:kind k8s-resp) "Deployment")
    (let [{:keys [name namespace]} (:metadata k8s-resp)]
      {:services-deployed [{:name name}]
       :namespace         namespace})
    {:success      false
     :k8s-response k8s-resp}))
