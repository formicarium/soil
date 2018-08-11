(ns soil.diplomat.kubernetes
  (:require [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.adapters.devspace :as adapters.devspace]
            [clojure.set :refer [difference]]
            [schema.core :as s]
            [soil.models.application :as models.application]))

(def tcp-ports (map str (range 4000 5000)))
(def tcp-config-map-name "my-nginx-nginx-ingress-tcp")
(def tcp-config-map-namespace "default")

(s/defn create-namespace! :- s/Str
  [namespace-name :- s/Str
   k8s-client :- protocols.k8s/KubernetesClient]
  (->> (adapters.devspace/devspace-name->create-namespace namespace-name)
       (protocols.k8s/create-namespace! k8s-client))
  namespace-name)

(s/defn create-deployment! :- models.application/Application
  [application :- models.application/Application
   k8s-client :- protocols.k8s/KubernetesClient]
  )

(s/defn create-services! :- models.application/Application
  [application :- models.application/Application
   k8s-client :- protocols.k8s/KubernetesClient]
  )

(s/defn create-ingress! :- models.application/Application
  [application :- models.application/Application
   k8s-client :- protocols.k8s/KubernetesClient]
  )

(defn get-tcp-available-ports
  [num k8s-client]
  (take num
    (->> (protocols.k8s/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace)
         :data
         keys
         (mapv name)
         set
         (difference (set tcp-ports)))))

(defn add-tcp-ports
  [services _ k8s-client]
  (prn "services" services)
  (protocols.k8s/patch-config-map! k8s-client
    tcp-config-map-name
    tcp-config-map-namespace
    {:data (zipmap
             (get-tcp-available-ports (count services) k8s-client)
             services)}))

(defn delete-tcp-ports
  [services k8s-client]
  (protocols.k8s/patch-config-map! k8s-client
    tcp-config-map-name
    tcp-config-map-namespace
    {:data (->> (protocols.k8s/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace)
                :data
                (filter (fn [[k v]] (some #(= (second (clojure.string/split v #"[/:]")) %) services)))
                (map (fn [[k v]] {k nil}))
                (into {}))}))

(defn get-nginx-tcp-config-map
  [k8s-client]
  (protocols.k8s/get-config-map k8s-client tcp-config-map-name tcp-config-map-namespace))
