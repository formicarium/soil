(ns controllers.devspace-test
  (:require [midje.sweet :refer :all]
            [soil.controllers.devspaces :as c-env]
            [soil.protocols.kubernetes.kubernetes-client :as p-k8s]
            [soil.logic.devspace :as l-env]
            [soil.config :as config]
            [soil.protocols.config.config :as p-cfg]))

#_(fact "Create devspace"
      (c-env/create-devspace! {:name ..env-name..} ..config.. ..k8s-client..) => ..env-created..
      (provided
        (p-k8s/create-namespace ..k8s-client.. ..env-name.. {:kind config/fmc-devspace-label}) => ..namespace..
        (p-k8s/create-deployment ..  )
        (l-env/namespace->devspace ..namespace..) => ..env-created..
        (l-env)))

(fact "List devspace"
  (c-env/list-devspaces ..k8s-client..) => {"bar" {:hiveUrl "http://hive.bar.formicarium.host"
                                                   :tanajuraApiUrl "http://tanajura.bar.formicarium.host"
                                                   :tanajuraGitUrl "http://git.bar.formicarium.host"}
                                            "foo" {:hiveUrl "http://hive.foo.formicarium.host"
                                                   :tanajuraApiUrl "http://tanajura.foo.formicarium.host"
                                                   :tanajuraGitUrl "http://git.foo.formicarium.host"}}
  (provided
    (p-k8s/list-namespaces ..k8s-client..) => ..ns-list..
    (l-env/namespaces->devspaces ..ns-list..) => [{:name "foo"} {:name "bar"}]))

(fact "Delete devspace"
  (c-env/delete-devspace {:name ..env-name..} ..k8s-client..) => {:success true}
  (provided
    (p-k8s/delete-namespace! ..k8s-client.. ..env-name..) => irrelevant))

