(ns soil.logic.application
  (:require [schema.core :as s]
            [soil.logic.interface :as logic.interface]
            [soil.models.application :as models.application]))

(s/defn get-container-interfaces :- [models.application/Interface]
  [{:application/keys [interfaces]} :- models.application/Application
   container :- models.application/Container]
  (->> interfaces
       (filter #(= (:container/name container) (:interface/container %)))))

(s/defn get-tcp-interfaces :- [models.application/Interface]
  [{:application/keys [interfaces]} :- models.application/Application]
  (filter #(= (:interface/type %) :interface.type/tcp) interfaces))

(s/defn get-non-tcp-interfaces :- [models.application/Interface]
  [{:application/keys [interfaces]} :- models.application/Application]
  (filter #(not= (:interface/type %) :interface.type/tcp) interfaces))

(s/defn make-container-syncable :- models.application/Container
  [{:application/keys [name]} :- models.application/Application
   {:container/keys [syncable? env] :as container} :- models.application/Container]
  (if syncable?
    (assoc container :container/env (merge env {"STARTUP_CLONE"    "true"
                                                "START_AFTER_PULL" "true"
                                                "STINGER_PORT"     "24000"
                                                "APP_PATH"         "/app"
                                                "STINGER_SCRIPTS"  "/scripts"
                                                "GIT_URI"          (str "http://tanajura:6666/" name ".git")})) ;; TODO: get tanajura-git from etcd
    container))

(s/defn get-syncable-container :- models.application/Container
  [containers :- [models.application/Container]]
  (->> (filter :container/syncable? containers)
       first))

(s/defn get-stinger-interface :- models.application/Interface
  [{:application/keys [name devspace containers]} :- models.application/Application
   domain :- s/Str]
  (logic.interface/new {:name      "stinger"
                        :devspace  devspace
                        :port      24000
                        :type      :http
                        :container (:container/name (get-syncable-container containers))
                        :service   name
                        :domain    domain}))

(s/defn make-syncable :- models.application/Application
  [application :- models.application/Application
   domain :- s/Str]
  (-> application
      (update :application/containers #(mapv (partial make-container-syncable application) %))
      (update :application/interfaces #(cons (get-stinger-interface application domain) %))))

(s/defn is-application-syncable?
  [{:application/keys [containers]} :- models.application/Application]
  (->> (mapv :container/syncable? containers)
       (some true?)))

(s/defn with-syncable-config :- models.application/Application
  [application :- models.application/Application
   domain :- s/Str]
  (if (is-application-syncable? application)
    (make-syncable application domain)
    application))
