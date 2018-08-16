(ns soil.logic.services
  (:require [schema.core :as s]
            [soil.components.config-server-client :as protocols.config-server-client]
            [soil.schemas.kubernetes.deployment :as schemas.kubernetes.deployment]))



(defn calc-host
  [hostname port-name namespace domain]
  (clojure.string/join "." [(str hostname (when-not (= port-name "default") (str "-" port-name))) namespace domain]))

(defn get-port-for-service
  [{:keys [name port]}]
  (if (= name "default")
    80
    port))

(s/defn config->ingress
  [service-configuration :- protocols.config-server-client/ServiceConfiguration
   namespace :- s/Str
   domain :- s/Str]
  (let [stinger-ports [{:port 24000 :name "stinger"}]
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
                                                          :http {:paths [{:backend {:serviceName (str service-name "-" name)
                                                                                    :servicePort name}
                                                                          :path    "/"}]}})))
                  :tls   [{:hosts      (vec (map (fn [{:keys [name]}] (calc-host hostname name namespace domain)) ports))
                           :secretName (str service-name "-certificate")}]}}))

(defn port->service
  [{:keys [name] :as port-description} service-name namespace]
  {:apiVersion "v1"
   :kind       "Service"
   :metadata   {:name      (str service-name "-" name)
                :labels    {:app service-name}
                :namespace namespace}
   :spec       {:ports    [{:protocol   "TCP"
                            :name       name
                            :port       80
                            :targetPort name}]
                :selector {:app service-name}}})

(defn config->services
  [service-configuration namespace]
  (let [service-name (:name service-configuration)
        stinger-ports [{:port 24000 :name "stinger"}]
        ports (concat (:ports service-configuration) stinger-ports)]
    (mapv #(port->service % service-name namespace) ports)))


(defn config->tcp-services
  [{:keys [ports name]} namespace]
  (->> ports
       (filter #(= (:type %) "tcp"))
       (map (fn [{:keys [port]}] (str namespace "/" name ":" port)))
       (vec)))

(defn get-repl-port [devspace-name service-name config-map]
  (some->> config-map
           :data
           (filter
             (fn [[_ service]]
               (clojure.string/includes? service (str devspace-name "/" service-name))))
           ffirst
           name
           Integer/parseInt))
