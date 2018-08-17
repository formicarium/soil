(ns controllers.devspace-test
  (:require [midje.sweet :refer :all]
            [soil.controllers.devspaces :as controllers.devspaces]
            [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.logic.devspace :as logic.devspaces]
            [clj-service.test-helpers :as th]
            [clj-service.protocols.config :as protocols.config]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]
            [schema.core :as s]))

(s/without-fn-validation
  (fact "Delete devspace"
        (controllers.devspaces/delete-devspace! ..env-name.. ..etcd.. ..k8s-client..) => irrelevant
        (provided
          (protocols.k8s/delete-namespace! ..k8s-client.. ..env-name..) => irrelevant)))

