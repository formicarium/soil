(ns soil.diplomat.kubernetes
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.protocols.config.config :as p-cfg]
            [clojure.set :refer [difference]]
            [schema.core :as s]
            [soil.models.kubernetes :as models.k8s]))

(def tcp-ports (map str (range 4000 5000)))
(def tcp-config-map-name "my-nginx-nginx-ingress-tcp")
(def default-backend "my-nginx-nginx-ingress-default-backend")
(def tcp-config-map-namespace "default")

(defn tap [any] (prn any) any)

(defn get-tcp-available-ports
  [num k8s-client]
  (take num
        (->> (p-k8s/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace)
             :data
             keys
             (mapv name)
             set
             (difference (set tcp-ports)))))

(defn add-tcp-ports
  [services config k8s-client]
  (prn "services" services)
  (p-k8s/patch-config-map! k8s-client
                           tcp-config-map-name
                           tcp-config-map-namespace
                           {:data (zipmap (tap (get-tcp-available-ports (count services) k8s-client))
                                          services)}))

(defn delete-tcp-ports
  [services k8s-client]
  (p-k8s/patch-config-map! k8s-client
                           tcp-config-map-name
                           tcp-config-map-namespace
                           {:data (->> (p-k8s/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace)
                                       :data
                                       (filter (fn [[k v]] (some #(= (second (clojure.string/split v #"[/:]")) %) services)))
                                       (map (fn [[k v]] {k nil}))
                                       (into {}))}))

(s/defn create-deployment!
  [deployment :- models.k8s/Deployment
   k8s-client]
  (p-k8s/create-deployment! k8s-client deployment))

(defn get-nginx-tcp-config-map
  [k8s-client]
  (p-k8s/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace))
