(ns soil.logic.interface
  (:require [schema.core :as s]
            [soil.models.application :as models.application]))

(s/defn calc-host :- s/Str
  [hostname :- s/Str
   port-name :- s/Str
   namespace :- s/Str
   domain :- s/Str]
  (clojure.string/join "." [(str hostname (when-not (= port-name "default") (str "-" port-name))) namespace domain]))

(s/defn new :- models.application/Interface
  [{:keys [name port type devspace service domain]
    :or   {name "default"
           type :interface.type/http}}]
  #:interface{:name      name
              :port      port
              :type      type
              :container service
              :host      (calc-host service name devspace domain)})
