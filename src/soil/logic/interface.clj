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
  [{:interface/keys [type]}]
  (boolean (#{:interface.type/nrepl :interface.type/tcp} type)))

(s/defn exposed? :- s/Bool
  [{:interface/keys [expose?]} :- models.application/Interface]
  (not (false? expose?)))
(s/defn tcp-entry
  [service-name devspace port]
  (str devspace "/" service-name ":" port))

(s/defn get-node-internal-ip :- (s/maybe s/Str)
  [node :- (s/pred map?)]
  (->> (get-in node [:status :addresses])
       (filter (fn [{:keys [type]}] (= type "InternalIP")))
       first
       :address))

(s/defn get-node-external-ip :- (s/maybe s/Str)
  [node :- (s/pred map?)]
  (->> (get-in node [:status :addresses])
       (filter (fn [{:keys [type]}] (= type "ExternalIP")))
       first
       :address))

(s/defn get-node-ip :- s/Str
  [node :- (s/pred map?)]
  (or (get-node-external-ip node)
      (get-node-internal-ip node)))

(s/defn render-interface-tcp-host :- models.application/Interface
  [interface :- models.application/Interface
   host :- s/Str]
  (assoc interface :interface/host host))

(s/defn render-interface
  [interface :- models.application/Interface
   tcp-hosts :- {s/Str s/Str}]
  (if (tcp-like? interface)
    (render-interface-tcp-host interface (get tcp-hosts (:interface/name interface)))
    interface))
