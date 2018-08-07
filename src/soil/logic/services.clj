(ns soil.logic.services
  (:require [schema.core :as s]
            [soil.components.configserver.configserver-client :as cfg-server]
            [soil.protocols.config.config :as p-cfg]
            [soil.components.kubernetes.schema.deployment :as k8s-schema-deploy]))

(s/defn config->deployment :- k8s-schema-deploy/Deployment
  [service-configuration namespace :- s/Str]
  (let [service-name (:name service-configuration)]
    {:apiVersion "apps/v1"
     :kind       "Deployment"
     :metadata   {:name      service-name
                  :labels    {:app service-name}
                  :namespace namespace}
     :spec       {:selector {:matchLabels {:app service-name}}
                  :replicas (or (:replicas service-configuration) 1)
                  :template {:metadata {:name      service-name
                                        :namespace namespace
                                        :labels    {:app service-name}}
                             :spec     {:containers [{:name  service-name
                                                      :image (or (:image service-configuration)
                                                                 (str "formicarium/chamber-" (:build-tool service-configuration) ":0.0.1"))
                                                      :ports (into [{:name          "stinger-api"
                                                                     :containerPort 24000}] (map (fn [port] {:name          (:name port)
                                                                                                             :containerPort (:port port)})
                                                                                                 (:ports service-configuration)))
                                                      :env   (concat
                                                               [{:name  "STARTUP_CLONE"
                                                                 :value "true"}
                                                                {:name  "STINGER_PORT"
                                                                 :value "24000"}
                                                                {:name  "APP_PATH"
                                                                 :value "/app"}
                                                                {:name  "STINGER_SCRIPTS"
                                                                 :value "/scripts"}
                                                                {:name  "GIT_URI"
                                                                 :value (str "http://git." namespace ".cluster.host/" service-name)}]
                                                               (mapv (fn [[k v]] {:name (name k) :value v})
                                                                     (:environment-variables service-configuration)))}]}}}}))

(defn calc-host
  [hostname port-name namespace domain]
  (clojure.string/join "." [(str hostname (when-not (= port-name "default") (str "-" port-name))) namespace domain]))

(s/defn config->ingress
  [service-configuration :- cfg-server/ServiceConfiguration
   namespace :- s/Str
   domain :- s/Str]
  (let [stinger-ports [{:port 24000 :name "stinger-api"}]
        ports (concat (:ports service-configuration) stinger-ports)
        service-name (:name service-configuration)
        hostname (or service-name (:host service-configuration))]
    {:apiVersion "extensions/v1beta1"
     :kind       "Ingress"
     :metadata   {:name        service-name
                  :annotations {"kubernetes.io/ingress.class" "nginx"}
                  :labels      {:app service-name}
                  :namespace   namespace}
     :spec       {:rules (->> ports
                              (mapv (fn [{:keys [name]}] {:host (calc-host hostname name namespace domain)
                                                          :http {:paths [{:backend {:serviceName service-name
                                                                                    :servicePort name}
                                                                          :path    "/"}]}})))
                  :tls   [{:hosts      (vec (map (fn [{:keys [name]}] (calc-host hostname name namespace domain)) ports))
                           :secretName (str service-name "-certificate")}]}}))

(defn config->service
  [service-configuration namespace]
  (let [service-name (:name service-configuration)
        stinger-ports [{:port 24000 :name (str service-name "-stinger")}]
        ports (concat (:ports service-configuration) stinger-ports)]
    {:apiVersion "v1"
     :kind       "Service"
     :metadata   {:name      service-name
                  :labels    {:app service-name}
                  :namespace namespace}
     :spec       {:ports    (->> ports
                                 (mapv (fn [{:keys [name port]}] {:protocol   "TCP"
                                                                  :name       name
                                                                  :port       (if (or (= name "default")
                                                                                      (= port 24000))
                                                                                80
                                                                                port)
                                                                  :targetPort name})))
                  :selector {:app service-name}}}))


(defn config->tcp-services
  [{:keys [ports name]} namespace]
  (->> ports
       (filter #(= (:type %) "tcp"))
       (map (fn [{:keys [port]}] (str namespace "/" name ":" port)))
       (vec)))

(defn config->kubernetes
  [service-configuration namespace domain]
  {:deployment   (config->deployment service-configuration namespace)
   :ingress      (config->ingress service-configuration namespace domain)
   :service      (config->service service-configuration namespace)
   :tcp-services (config->tcp-services service-configuration namespace)})

(s/defn gen-hive-deployment
  [devspace :- s/Str
   config]
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:name      "hive"
                :namespace devspace}
   :spec       {:selector {:matchLabels {:app "hive"}}
                :replicas 1
                :template {:metadata {:labels {:app "hive"}}
                           :spec     {:containers [{:name  "hive"
                                                    :image (str "formicarium/hive:" (p-cfg/get-config config [:hive :version]))
                                                    :ports [{:name          "hive-api"
                                                             :containerPort 8080}
                                                            {:name          "hive-repl"
                                                             :containerPort 2222}
                                                            {:name          "hive-tracing"
                                                             :containerPort 9898}]}]}}}})

(defn gen-hive-service
  [namespace]
  {:apiVersion "v1"
   :kind       "Service"
   :metadata   {:name      "hive"
                :labels    {:app "hive"}
                :namespace namespace}
   :spec       {:ports    [{:protocol   "TCP"
                            :name       "hive-api"
                            :port       80
                            :targetPort "hive-api"}
                           {:protocol   "TCP"
                            :name       "hive-repl"
                            :port       2222
                            :targetPort "hive-repl"}
                           {:protocol   "TCP"
                            :name       "hive-tracing"
                            :port       9898
                            :targetPort "hive-tracing"}]
                :selector {:app "hive"}}})

(defn gen-hive-ingress
  [namespace config]
  (let [domain (p-cfg/get-config config [:formicarium :domain])]
    {:apiVersion "extensions/v1beta1"
     :kind       "Ingress"
     :metadata   {:name        "hive"
                  :annotations {"kubernetes.io/ingress.class" "nginx"}
                  :labels      {:app "hive"}
                  :namespace   namespace}
     :spec       {:rules [{:host (calc-host "hive" "default" namespace domain)
                           :http {:paths [{:backend {:serviceName "hive"
                                                     :servicePort "hive-api"}
                                           :path    "/"}]}}
                          {:host (calc-host "hive" "tracing" namespace domain)
                           :http {:paths [{:backend {:serviceName "hive"
                                                     :servicePort "hive-tracing"}
                                           :path    "/"}]}}]
                  :tls   [{:hosts      [(calc-host "hive" "default" namespace domain)
                                        (calc-host "hive" "tracing" namespace domain)]
                           :secretName "hive-certificate"}]}}))

(defn gen-hive-tcp-service
  [namespace]
  [(str namespace "/hive:2222")])

(defn hive->kubernetes
  [namespace config]
  {:deployment   (gen-hive-deployment namespace config)
   :ingress      (gen-hive-ingress namespace config)
   :service      (gen-hive-service namespace)
   :tcp-services (gen-hive-tcp-service namespace)})

(defn build-response
  [k8s-resp]
  (if (= (:kind k8s-resp) "Deployment")
    (let [{:keys [name namespace]} (:metadata k8s-resp)]
      {:services-deployed [{:name name}]
       :namespace         namespace})
    {:success      false
     :k8s-response k8s-resp}))

(defn gen-tanajura-deployment [devspace config]
  {:apiVersion "apps/v1"
   :kind       "Deployment"
   :metadata   {:name      "tanajura"
                :namespace devspace}
   :spec       {:selector {:matchLabels {:app "tanajura"}}
                :replicas 1
                :template {:metadata {:labels {:app "tanajura"}}
                           :spec     {:containers [{:name  "hive"
                                                    :image (str "formicarium/tanajura:" (p-cfg/get-config config [:tanajura :version]))
                                                    :ports [{:name          "tanajura-api"
                                                             :containerPort 3002}
                                                            {:name          "tanajura-git"
                                                             :containerPort 6666}]}]}}}})

(defn gen-tanajura-ingress [devspace config]
  (let [domain (p-cfg/get-config config [:formicarium :domain])]
    {:apiVersion "extensions/v1beta1"
     :kind       "Ingress"
     :metadata   {:name        "tanajura"
                  :annotations {"kubernetes.io/ingress.class" "nginx"}
                  :labels      {:app "tanajura"}
                  :namespace   devspace}
     :spec       {:rules [{:host (clojure.string/join "." ["git" devspace domain])
                           :http {:paths [{:backend {:serviceName "tanajura"
                                                     :servicePort "tanajura-git"}
                                           :path    "/"}]}}
                          {:host (calc-host "tanajura" "default" devspace domain)
                           :http {:paths [{:backend {:serviceName "tanajura"
                                                     :servicePort "tanajura-api"}
                                           :path    "/"}]}}]}}))

(defn gen-tanajura-service [devspace]
  {:apiVersion "v1"
   :kind       "Service"
   :metadata   {:name      "tanajura"
                :labels    {:app "tanajura"}
                :namespace devspace}
   :spec       {:ports    [{:protocol   "TCP"
                            :name       "tanajura-api"
                            :port       80
                            :targetPort "tanajura-api"}
                           {:protocol   "TCP"
                            :name       "tanajura-git"
                            :port       6666
                            :targetPort "tanajura-git"}]
                :selector {:app "tanajura"}}})

(defn tanajura->kubernetes [namespace config]
  {:deployment   (gen-tanajura-deployment namespace config)
   :ingress      (gen-tanajura-ingress namespace config)
   :service      (gen-tanajura-service namespace)
   :tcp-services []})
