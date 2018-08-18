(ns soil.logic.interface
  (:require [schema.core :as s]
            [soil.models.application :as models.application]))

(s/defn is-default? :- s/Bool
  [n :- (s/either s/Str s/Keyword)]
  (= (name n) "default"))

(s/defn calc-name :- s/Str
  [service-name :- s/Str
   interface-name :- (s/either s/Str s/Keyword)]
  (str service-name (when-not (is-default? interface-name) (str "-" (name interface-name)))))

(s/defn calc-host :- s/Str
  [service-name :- s/Str
   interface-name :- s/Str
   devspace-name :- s/Str
   domain :- s/Str]
  (clojure.string/join "." [(calc-name service-name interface-name) devspace-name domain]))

(s/defn new :- models.application/Interface
  [{:keys [name port type devspace container service domain]
    :or   {name "default"
           type :interface.type/http}}]
  #:interface{:name      name
              :port      port
              :type      type
              :container container
              :host      (calc-host service name devspace domain)})

(s/defn http-like? :- s/Bool
  [{:interface/keys [type]} :- models.application/Interface]
  (boolean (#{:interface.type/http :interface.type/https} type)))

(s/defn tcp-like? :- s/Bool
  [{:interface/keys [type]} :- models.application/Interface]
  (boolean (#{:interface.type/nrepl :interface.type/tcp} type)))

(s/defn exposed? :- s/Bool
  [{:interface/keys [expose?]} :- models.application/Interface]
  (not (false? expose?)))
