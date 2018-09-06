(ns controllers.devspace-test
  (:require [midje.sweet :refer :all]
            [soil.controllers.devspaces :as controllers.devspaces]
            [soil.protocols.kubernetes-client :as protocols.k8s]
            [schema.core :as s]
            [soil.protocols.etcd :as protocols.etcd]
            [soil.controllers.devspaces :as controllers.devspace]))

(s/without-fn-validation
  (fact "Delete devspace"
    (controllers.devspaces/delete-devspace! "test" ..etcd.. ..k8s-client..) => irrelevant
    (provided
      (controllers.devspace/check-if-devspace-exists "test" ..etcd..) => "test"
      (protocols.etcd/delete-prefix! ..etcd.. "applications/test") => irrelevant
      (protocols.etcd/delete! ..etcd.. "devspaces/test") => irrelevant
      (protocols.k8s/delete-namespace! ..k8s-client.. "test") => irrelevant)))

