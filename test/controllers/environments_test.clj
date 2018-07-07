(ns controllers.environments-test
  (:require [midje.sweet :refer :all]
            [soil.controllers.environments :as c-env]
            [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.logic.environment :as l-env]))

(fact "Create enviroment"
      (c-env/create-environment {:name ..env-name..} ..k8s-client..) => ..env-created..
      (provided
       (p-k8s/create-namespace ..k8s-client.. ..env-name..) => ..namespace..
       (l-env/namespace->environment ..namespace..) => ..env-created..))

(fact "List environments"
      (c-env/list-environments ..k8s-client..) => ..env-list..
      (provided
       (p-k8s/list-namespaces ..k8s-client..) => ..ns-list..
       (l-env/namespaces->environments ..ns-list..) => ..env-list..))

(fact "Delete environment"
      (c-env/delete-environment {:name ..env-name..} ..k8s-client..) => {:success true}
      (provided
       (p-k8s/delete-namespace ..k8s-client.. ..env-name..) => irrelevant))

