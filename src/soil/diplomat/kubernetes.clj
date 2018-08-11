(ns soil.diplomat.kubernetes
  (:require [soil.protocols.kubernetes-client :as protocols.kuberentes-client]
            [clojure.set :refer [difference]]))

(def tcp-ports (map str (range 4000 5000)))
(def tcp-config-map-name "my-nginx-nginx-ingress-tcp")
(def tcp-config-map-namespace "default")

(defn tap [any] (prn any) any)

(defn get-tcp-available-ports
  [num k8s-client]
  (take num
    (->> (protocols.kuberentes-client/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace)
         :data
         keys
         (mapv name)
         set
         (difference (set tcp-ports)))))

(defn add-tcp-ports
  [services _ k8s-client]
  (prn "services" services)
  (protocols.kuberentes-client/patch-config-map! k8s-client
    tcp-config-map-name
    tcp-config-map-namespace
    {:data (zipmap (tap (get-tcp-available-ports (count services) k8s-client))
             services)}))

(defn delete-tcp-ports
  [services k8s-client]
  (protocols.kuberentes-client/patch-config-map! k8s-client
    tcp-config-map-name
    tcp-config-map-namespace
    {:data (->> (protocols.kuberentes-client/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace)
                :data
                (filter (fn [[k v]] (some #(= (second (clojure.string/split v #"[/:]")) %) services)))
                (map (fn [[k v]] {k nil}))
                (into {}))}))

(defn get-nginx-tcp-config-map
  [k8s-client]
  (protocols.kuberentes-client/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace))
