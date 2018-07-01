(ns soil.k8s
  (:require [kubernetes.api.v1 :as k8s]))


(def ctx (k8s/make-context "http://localhost:9090"))