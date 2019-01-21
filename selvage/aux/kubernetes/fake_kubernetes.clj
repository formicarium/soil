(ns aux.kubernetes.fake-kubernetes
  (:require [clojure.test :refer :all]
            [org.httpkit.fake :refer [with-fake-http]]))

(defn create-deployment [url namespace-name]
  {:url (str url "/apis/apps/v1/namespaces/" namespace-name "/deployments")
   :method :post})

(defn create-service [url namespace-name]
  {:url (str url "/api/v1/namespaces/" namespace-name "/services")
   :method :post})

(defn create-ingress [url namespace-name]
  {:url (str url "/apis/extensions/v1beta1/namespaces/" namespace-name "/ingresses")
   :method :post})

(defn get-pods [url namespace-name]
  {:url (str url "/api/v1/namespaces/" namespace-name "/pods")
   :method :get})

(defn get-nodes [url]
  {:url (str url "/api/v1/nodes")
   :method :get})

(defn get-service [url namespace-name service-name]
  {:url (str url "/api/v1/namespaces/" namespace-name "/services/" service-name)
   :method :get})

(defn with-fake-kubernetes
  [spec & body]
  (with-fake-http (flatten spec) body))
