(ns soil.logic.devspace
  (:require [soil.config :as config]))

(defn namespace->devspace
  [namespace]
  {:name (get-in namespace [:metadata :name])})

(defn namespaces->devspaces
  [namespaces]
  (->> (filter #(= config/fmc-devspace-label (-> % :metadata :labels :kind)) namespaces)
       (map namespace->devspace)))

