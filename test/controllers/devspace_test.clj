(ns controllers.devspace-test
  (:require [midje.sweet :refer :all]
            [soil.controllers.devspaces :as c-env]
            [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.logic.devspace :as l-env]))

(fact "Create devspace"
      (c-env/create-devspace {:name ..env-name..} ..k8s-client..) => ..env-created..
      (provided
        (p-k8s/create-namespace ..k8s-client.. ..env-name..) => ..namespace..
        (l-env/namespace->devspace ..namespace..) => ..env-created..))

(fact "List devspace"
      (c-env/list-devspaces ..k8s-client..) => ..env-list..
      (provided
        (p-k8s/list-namespaces ..k8s-client..) => ..ns-list..
        (l-env/namespaces->devspaces ..ns-list..) => ..env-list..))

(fact "Delete devspace"
      (c-env/delete-devspace {:name ..env-name..} ..k8s-client..) => {:success true}
      (provided
       (p-k8s/delete-namespace ..k8s-client.. ..env-name..) => irrelevant))
