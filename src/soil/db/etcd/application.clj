(ns soil.db.etcd.application
  (:require [soil.protocols.etcd :as protocols.etcd]
            [schema.core :as s]
            [soil.models.application :as models.application]))

(s/defn ^:private get-path-for-application
  [devspace application-name]
  (clojure.string/join "/" ["" "applications" devspace application-name]))

(s/defn create-application!
  [{:application/keys [name devspace] :as application} :- models.application/Application
   etcd :- protocols.etcd/IEtcd]
  (protocols.etcd/put! etcd (clojure.string/join "/" ["applications" devspace name]) application))

(s/defn get-application! :- models.application/Application
  [application-name :- s/Str
   devspace :- s/Str
   etcd :- protocols.etcd/IEtcd]
  (protocols.etcd/get! etcd (get-path-for-application devspace application-name)))
