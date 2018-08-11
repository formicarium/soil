(ns soil.logic.interface
  (:require [schema.core :as s]
            [soil.models.application :as models.application]))

(s/defn calc-host :- s/Str
  [hostname :- s/Str
   port-name :- s/Str
   namespace :- s/Str
   domain :- s/Str]
  (clojure.string/join "." [(str hostname (when-not (= (name port-name) "default") (str "-" (name port-name)))) namespace domain]))

(s/defn calc-name :- s/Str
  [service :- s/Str
   interface-name :- s/Str]
  (clojure.string/join "-" [(str service (when-not (= (name interface-name) "default") (str "-" (name interface-name))))]))

(s/defn new :- models.application/Interface
  [{:keys [name port type devspace container service domain]
    :or   {name "default"
           type :interface.type/http}}]
  #:interface{:name      (calc-name service name)
              :port      port
              :type      type
              :container container
              :host      (calc-host service name devspace domain)})
