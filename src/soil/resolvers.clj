(ns soil.resolvers
  (:require [soil.controllers :as controllers]))


(defn get-namespaces [_ _ _]
  (controllers/get-namespaces))


(defn get-namespace [_ {:keys [namespace]} _]
  (controllers/get-namespace namespace))


(defn create-namespace [_ {:keys [namespace]} _]
  (controllers/create-namespace namespace))


(defn delete-namespace [_ {:keys [namespace]} _]
  (controllers/delete-namespace namespace))

(defn deploy [_ {:keys [namespace serviceName shard image]} _]
  (controllers/deploy namespace serviceName shard image))