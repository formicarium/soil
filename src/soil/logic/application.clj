(ns soil.logic.application
  (:require [schema.core :as s]
            [soil.logic.interface :as logic.interface]
            [soil.models.application :as models.application]))

(s/defn get-container-interfaces :- [models.application/Interface]
  [{:application/keys [interfaces]} :- models.application/Application
   container :- models.application/Container]
  (->> interfaces
       (filter #(= (:container/name container) (:interface/container %)))))

(s/defn get-tcp-like-interfaces :- [models.application/Interface]
  [{:application/keys [interfaces]} :- models.application/Application]
  (filter logic.interface/tcp-like? interfaces))

(s/defn get-http-like-interfaces :- [models.application/Interface]
  [{:application/keys [interfaces]} :- models.application/Application]
  (filter logic.interface/http-like? interfaces))

(s/defn has-http-like-interfaces :- s/Bool
  [application :- models.application/Application]
  (> (count (get-http-like-interfaces application)) 0))

(s/defn has-tcp-like-interfaces :- s/Bool
  [application :- models.application/Application]
  (> (count (get-tcp-like-interfaces application)) 0))

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
                        :type      :interface.type/http
                        :container (:container/name (get-syncable-container containers))
                        :service   name
                        :domain    domain}))

(s/defn make-syncable :- models.application/Application
  [application :- models.application/Application
   domain :- s/Str]
  (-> application
      (update :application/containers #(mapv (partial make-container-syncable application) %))
      (update :application/interfaces #(cons (get-stinger-interface application domain) %))))

(s/defn syncable?
  [{:application/keys [containers]} :- models.application/Application]
  (->> (mapv :container/syncable? containers)
       (some true?)))

(s/defn with-syncable-config :- models.application/Application
  [application :- models.application/Application
   domain :- s/Str]
  (if (syncable? application)
    (make-syncable application domain)
    application))

(s/defn hive? :- s/Bool
  [application :- models.application/Application]
  (= "hive" (:application/name application)))

(s/defn tanajura? :- s/Bool
  [application :- models.application/Application]
  (= "tanajura" (:application/name application)))

(s/defn get-hive :- (s/maybe models.application/Application)
  [applications :- [models.application/Application]]
  (first (filter hive? applications)))

(s/defn get-tanajura :- (s/maybe models.application/Application)
  [applications :- [models.application/Application]]
  (first (filter tanajura? applications)))

(s/defn but-hive-tanajura :- [models.application/Application]
  [applications :- [models.application/Application]]
  (remove #(or (tanajura? %) (hive? %)) applications))

(s/defn get-config-map :- {:data {s/Keyword s/Str}}
  [{:application/keys [name devspace] :as application} :- models.application/Application
   ports :- [s/Int]]
  {:data (->> (get-tcp-like-interfaces application)
              (mapv (fn [{:interface/keys [port]}] (logic.interface/tcp-entry name devspace port)))
              (zipmap (mapv (comp keyword str) ports)))})

(s/defn get-erase-config-map :- {:data {s/Keyword s/Str}}
  [application :- models.application/Application
   ports :- [s/Int]]
  {:data (->> (repeat (count (get-tcp-like-interfaces application)) nil)
              (zipmap (mapv (comp keyword str) ports)))})

(s/defn get-interface-by-name
  [{:application/keys [interfaces]} :- models.application/Application
   interface-name :- s/Str]
  (filter #(= interface-name (:interface/name %)) interfaces))

(s/defn render-tcp-hosts :- models.application/Application
  [{:application/keys [interfaces] :as application} :- models.application/Application
   tcp-hosts :- {s/Str s/Str}]
  (assoc application :application/interfaces
                     (mapv (fn [interface] (logic.interface/render-interface interface tcp-hosts)) interfaces)))
