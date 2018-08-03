(ns soil.diplomat.kubernetes
  (:require [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.protocols.config.config :as p-cfg]
            [clojure.set :refer [difference]]))

(def tcp-ports (map str (range 4000 5000)))
(def tcp-config-map-name "my-nginx-nginx-ingress-tcp")
(def tcp-config-map-namespace "default")

(defn tap [any] (prn any) any)

(defn get-tcp-available-ports
  [num k8s-client]
  (take num
        (->> (p-k8s/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace)
             :data
             keys
             (map name)
             set
             (difference (set tcp-ports)))))

(defn add-tcp-ports
  [services config k8s-client]
  (prn "services" services)
  (p-k8s/patch-config-map k8s-client
                          tcp-config-map-name
                          tcp-config-map-namespace
                          {:data (->> (map vector (get-tcp-available-ports (count services) k8s-client) services)
                                      (map (fn [[k v]] {(str k) v}))
                                      (into {}))}))

(defn delete-tcp-ports
  [services k8s-client]
  (p-k8s/patch-config-map k8s-client
                          tcp-config-map-name
                          tcp-config-map-namespace
                          {:data (->> (p-k8s/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace)
                                      :data
                                      (filter (fn [[k v]] (some #(= (second (clojure.string/split v #"[/:]")) %) services)))
                                      (map (fn [[k v]] {k nil}))
                                      (into {}))}))
