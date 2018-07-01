(ns soil.resolvers
  (:require [soil.controllers :as controllers]))

(defn internal->external [{:keys [service-name]:as internal}]
  {:name "xxx"})


(defn get-namespaces [_ _ _]
  (controllers/get-namespaces))


(defn get-namespace [_ {:keys [namespace]} _]
  (println "ok")
  (controllers/get-namespace namespace))


(defn create-namespace [_ {:keys [namespace]} _]
  (controllers/create-namespace namespace))


(defn delete-namespace [_ {:keys [namespace]} _]
  (controllers/delete-namespace namespace))

(defn deploy [_ {:keys [namespace name shard image]} _]
  (controllers/deploy namespace name shard image))