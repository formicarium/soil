(ns spin-env
  (:require [selvage.flow :refer [flow *world*]]
            [midje.sweet :refer :all]
            [soil.components :as soil]
            [com.stuartsierra.component :as component]
            [cheshire.core :as cheshire]
            [io.pedestal.http :as http]
            [clj-service.test-helpers :as th]
            [io.pedestal.test :refer [response-for]]
            [aero.core :as aero]
            [aux.kubernetes.fake-kubernetes :as fake-kubernetes]
            [aux.kubernetes.kubernetes-data :as kubernetes-data]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [clojure.java.io :as io])
  (:use org.httpkit.fake))
(def test-config (aero/read-config (io/resource "test.edn")))
(def kubernetes-proxy-url (get-in test-config [:kubernetes :url]))
(def configserver-url (get-in test-config [:config-server :url]))

(defn expose-service
  [world]
  (assoc world :service-fn (th/get-pedestal-service (:system world))))

(defn init!
  [world]
  (-> world
      (assoc :system (component/start-system (soil/system-map :test)))
      expose-service))

(defn end!
  [world]
  (-> world
      :system
      component/stop-system)
  (dissoc world :system))

(defn json->str [coll] (cheshire/generate-string coll))

(defn str->json [str] (cheshire/parse-string str true))

(defn fake-deployment-create-request
  [body]
  {:url    (str kubernetes-proxy-url "/apis/apps/v1/namespaces/carlos/deployments")
   :method :post} {:status 200
                   :body   (json->str body)})

(defn fake-service-create-request
  [body]
  {:url    (str kubernetes-proxy-url "/api/v1/namespaces/carlos/services")
   :method :post} {:status 200
                   :body   (json->str body)})

(defn create-env-req! [service] (response-for service
                                              :post "/api/devspaces"
                                              :headers {"Content-Type" "application/json"}
                                              :body (json->str {:name "carlos"})))

(def success {:kind "Status"
              :status "Success"})

(defn create-env!
  [world]
  (assoc world :env-created
               (with-fake-http [{:url    (str kubernetes-proxy-url "/api/v1/namespaces")
                                 :method :post} {:body (json->str {:apiVersion "v1"
                                                                   :kind       "Namespace"
                                                                   :metadata   {:name "carlos"
                                                                                :kind "fmc-devspace"}})}
                                {:url (str kubernetes-proxy-url "/apis/apps/v1/namespaces/carlos/deployments")
                                 :method :post} {:body (json->str success)}
                                (fake-kubernetes/create-service kubernetes-proxy-url "carlos") {:body (json->str success)}
                                (fake-kubernetes/create-ingress kubernetes-proxy-url "carlos") {:body (json->str success)}
                                (fake-kubernetes/get-nodes kubernetes-proxy-url) {:body {:apiVersion "v1"
                                                                                         :kind "ListNodes"
                                                                                         :items kubernetes-data/nodes}}
                                (fake-kubernetes/get-pods kubernetes-proxy-url "carlos") {:body {:apiVersion "v1"
                                                                                                 :kind "ListPods"
                                                                                                 :items kubernetes-data/pods}}
                                (fake-kubernetes/get-service kubernetes-proxy-url "carlos" "hive") {:body {:apiVersion "v1"
                                                                                                           :kind "Service"
                                                                                                           :metadata {:name "hive"
                                                                                                                      :namespace "carlos"
                                                                                                                      :labels {:formicarium.io/application "hive"},
                                                                                                                      :annotations {:formicarium.io/patches "[]",
                                                                                                                                    :formicarium.io/port-types "{\"default\" :interface.type/http, \"repl\" :interface.type/nrepl, \"zmq\" :interface.type/tcp}"}}
                                                                                                           :spec {:ports
                                                                                                                  [{:name "default", :protocol "TCP", :port 8080, :targetPort "default", :nodePort 31305}
                                                                                                                   {:name "repl", :protocol "TCP", :port 2222, :targetPort "repl", :nodePort 30292}
                                                                                                                   {:name "zmq", :protocol "TCP", :port 9898, :targetPort "zmq", :nodePort 32372}]
                                                                                                                  :selector {:formicarium.io/application "hive"}
                                                                                                                  :type "NodePort"}}}
                                (fake-kubernetes/get-service kubernetes-proxy-url "carlos" "tanajura") {:body {:kind "Service",
                                                                                                               :apiVersion "v1",
                                                                                                               :metadata {:name "tanajura",
                                                                                                                          :namespace "leal",
                                                                                                                          :labels {:formicarium.io/application "tanajura"},
                                                                                                                          :annotations {:formicarium.io/patches "[]",
                                                                                                                                        :formicarium.io/port-types "{\"default\" :interface.type/http, \"git\" :interface.type/http}"}},
                                                                                                               :spec {:ports [{:name "default", :protocol "TCP", :port 3002, :targetPort "default", :nodePort 31386}
                                                                                                                              {:name "git", :protocol "TCP", :port 6666, :targetPort "git", :nodePort 32160}],
                                                                                                                      :selector {:formicarium.io/application "tanajura"},
                                                                                                                      :type "NodePort"}}}

                                {:url    (str configserver-url "/api/devspace/create")
                                 :method :post} {:status 200 :body   (json->str [])}]
                               (create-env-req! (:service-fn world)))))

(def deployment-example {:apiVersion "apps/v1"
                         :kind       "Deployment"
                         :metadata   {:name      "nginx"
                                      :namespace "carlos"}
                         :spec       {:replicas 1
                                      :selector {:matchLabels {:app "nginx"}}
                                      :template {:metadata {:name      "nginx"
                                                            :namespace "carlos"
                                                            :labels    {:app "nginx"}}
                                                 :spec     {:containers [{:name  "nginx"
                                                                          :image "nginx:1.10"
                                                                          :ports [{:containerPort 80}]}]}}}})

(def service-args {:name      "nginx"
                   :randomOps 42})

(def service-configuration {:environment-variables {:foo "foo"}
                            :name                  "nginx-svc"
                            :image                 "nginx:1.10"})


(defn create-service-req! [service]
  (response-for service :post "/api/services"
                :headers {"Content-Type"         "application/json"
                          "Formicarium-Devspace" "carlos"}
                :body (json->str service-args)))

(defn create-service!
  [world]
  (assoc world :services-deployed (with-fake-http [{:url    (str kubernetes-proxy-url "/apis/apps/v1/namespaces/carlos/deployments")
                                                    :method :post} {:status 200
                                                                    :body   (json->str deployment-example)}
                                                   {:url    (str kubernetes-proxy-url "/api/v1/namespaces/carlos/services")
                                                    :method :post} {:body (json->str {:apiVersion "v1"
                                                                                      :kind       "Service"
                                                                                      :metadata   {:name "carlos"}})}
                                                   {:url    (str kubernetes-proxy-url "/apis/extensions/v1beta1/namespaces/carlos/ingresses")
                                                    :method :post} {:body (json->str {:apiVersion "v1"
                                                                                      :kind       "Ingress"
                                                                                      :metadata   {:name "carlos"}})}
                                                   {:url    (str configserver-url "/service/deploy")
                                                    :method :post
                                                    :body   (json->str service-args)} {:status 200
                                                                                       :body   (json->str service-configuration)}]
                                                  (create-service-req! (:service-fn world)))))

(flow "spin up a new devspace"
      init!
      (fn [world] (let [service (:service-fn world)]
                    (assoc world :service-health (response-for service :get "/api/health"))))

      (fact "health must answer 200"
            (:service-health *world*) => (match {:status 200
                                                 :body   (json->str {:healthy true})}))
      create-env!
      (facts "about devspace creation"
        (let [env-created (:env-created *world*)
              body (str->json (:body env-created))]
          (fact "returns created status"
            env-created => (match {:status 201}))
          (fact "returns devspace name"
            body => (match {:name "carlos"}))
          (fact "returns hive links"
            body => (match {:hive {:links {:repl "nrepl://10.129.218.235:30292"
                                           :zmq "tcp://10.129.218.235:32372"
                                           :default "http://hive.carlos.formicarium.host"}}}))
          (fact "returns tanajura links"
            body => (match {:tanajura {:links {:default "http://tanajura.carlos.formicarium.host",
                                               :git "http://tanajura-git.carlos.formicarium.host"}}}))))
      #_create-service!
      #_(fact "service 'nginx' must be deployed"
              (:services-deployed *world*) => (contains {:status 200}))
      end!)
