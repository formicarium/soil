(ns soil.logic.services
  (:require [schema.core :as s]))

(defn get-repl-port [devspace-name service-name config-map]
  (some->> config-map
           :data
           (filter
             (fn [[_ service]]
               (clojure.string/includes? service (str devspace-name "/" service-name))))
           ffirst
           name
           Integer/parseInt))
