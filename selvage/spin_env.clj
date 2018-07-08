(ns spin-env
  (:require [selvage.flow :refer [flow *world*]]
            [midje.sweet :refer :all]
            [clj-http.client :as http]
            [soil.component :as soil]
            [com.stuartsierra.component :as component]
            [cheshire.core :as cheshire])
  (:use clj-http.fake))

(defn init!
  [world]
  (assoc world :system (future (component/start-system soil/system-map))))


(defn to-json [coll] (cheshire/generate-string coll))

(defn from-json [str] (cheshire/parse-string str true))


(defn create-env-req! [] (http/post "http://localhost:8080/api/environments" {:content-type :json
                                                                              :body (to-json {:name "carlos"})}))

(defn create-env!
  [world]
  (assoc world :env (with-fake-routes
                      {"http://localhost:9000/api/v1/namespaces" {:post (fn [req]
                                                                          {:status 200
                                                                           :header {}
                                                                           :body   (to-json {:apiVersion "v1"
                                                                                             :kind       "Namespace"
                                                                                             :metadata   {:name "carlos"}})})}}
                      (create-env-req!))))

(flow "spin up a new environment"
      init!
      create-env!
      (fact "Foo must bar"
            (:env *world*) => "{:important true}"))
