(ns soil.protocols.config.config
  (:require [schema.core :as s]))

(defprotocol Config
  (get-config [this path]))

(def IConfig (:on-interface Config))
