(ns controllers.devspace-test
  (:require [midje.sweet :refer :all]
            [soil.controllers.devspaces :as controllers.devspaces]
            [soil.protocols.kubernetes-client :as protocols.k8s]
            [soil.logic.devspace :as logic.devspaces]
            [clj-service.test-helpers :as th]
            [clj-service.protocols.config :as protocols.config]
            [soil.diplomat.kubernetes :as diplomat.kubernetes]))

#_(fact "Create devspace"
        (controllers.devspaces/create-devspace! {:name ..env-name..} ..config.. ..k8s-client..) => ..env-created..
        (provided
          (p-k8s/create-namespace ..k8s-client.. ..env-name.. {:kind config/fmc-devspace-label}) => ..namespace..
          (p-k8s/create-deployment ..)
          (logic.devspaces/namespace->devspace ..namespace..) => ..env-created..
          (l-env)))
(def config (th/mock-config {:formicarium {:domain "formicarium.host"}}))
(fact "List devspace"
      (controllers.devspaces/list-devspaces ..k8s-client.. ..config..)
      => {"bar" {:configServerUrl "http://config-server"
                 :hiveApiUrl "http://hive.bar.formicarium.host"
                 :hiveReplUrl "nrepl://hive.bar.formicarium.host:9001"
                 :tanajuraApiUrl "http://tanajura.bar.formicarium.host"
                 :tanajuraGitUrl "http://git.bar.formicarium.host"}
          "foo" {:configServerUrl "http://config-server"
                 :hiveApiUrl "http://hive.foo.formicarium.host"
                 :hiveReplUrl "nrepl://hive.foo.formicarium.host:9000"
                 :tanajuraApiUrl "http://tanajura.foo.formicarium.host"
                 :tanajuraGitUrl "http://git.foo.formicarium.host"}}
      (provided
        (protocols.config/get-in! ..config.. [:formicarium :domain]) => "formicarium.host"
        (protocols.config/get-in! ..config.. [:config-server :url]) => "http://config-server"
        (protocols.k8s/list-namespaces ..k8s-client..) => ..ns-list..
        (diplomat.kubernetes/get-nginx-tcp-config-map ..k8s-client..) => {:data {:9000 "foo/hive:9898"
                                                                                 :9001 "bar/hive:9898"
                                                                                 :9002 "baz/hive:9898"}}
        (logic.devspaces/namespaces->devspaces ..ns-list..) => [{:name "foo"} {:name "bar"}]))

(fact "Delete devspace"
      (controllers.devspaces/delete-devspace ..env-name.. ..k8s-client..) => {:success true}
      (provided
        (protocols.k8s/delete-namespace! ..k8s-client.. ..env-name..) => irrelevant))

