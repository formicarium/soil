(ns soil.config
  (:require [clj-service.protocols.config :as protocols.config]
            [schema.core :as s]))

(def fmc-devspace-label "fmc-devspace")

(s/defn version :- s/Str
  [config :- protocols.config/IConfig]
  (protocols.config/get-in! config [:soil :version]))
