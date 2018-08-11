(ns soil.adapters.application
  (:require [schema.core :as s]
            [soil.models.application :as models.application]))

(s/defn application+container->ports :- [(s/pred map?)]
  [{:application/keys [interfaces]} :- models.application/Application
   container :- models.application/Container]
  (->> interfaces
       (filter #(= (:container/name container) (:interface/container %)))
       (mapv #(do {:name          (:interface/name %)
                   :containerPort (:interface/port %)}))))

(s/defn application->containers :- [(s/pred map?)]
  [{:application/keys [containers] :as application} :- models.application/Application]
  (mapv (fn [container]
          {:name  (:container/name container)
           :image (:container/image container)
           :ports (application+container->ports application container)
           :env   (mapv #(do {:name  (clojure.core/name (key %))
                              :value (str (val %))}) (:container/env container))}) containers))

(s/defn application->deployment :- (s/pred map?)
  [{:application/keys [devspace] :as application} :- models.application/Application]
  (let [app-name (:application/name application)]
    {:apiVersion "apps/v1"
     :kind       "Deployment"
     :metadata   {:name      app-name
                  :labels    {:app app-name}
                  :namespace devspace}
     :spec       {:selector {:matchLabels {:app app-name}}
                  :replicas 1
                  :template {:metadata {:labels {:app app-name}}
                             :spec     {:containers (application->containers application)}}}}))
