(ns soil.logic.kubernetes
  (:require [schema.core :as s]
            [clj-service.misc :as misc]))

(s/defn res->app-name :- (s/maybe s/Str)
  [res :- (s/pred map?)]
  (get-in res [:metadata :labels "formicarium.io/application"]))

(s/defn find-by-app-name :- (s/maybe (s/pred map?))
  [app-name :- s/Str
   res :- [(s/pred map?)]]
  (misc/find-first res #(= app-name (res->app-name %))))

(s/defn persistent-volume-claim
  [pvc-name :- s/Str
   pvc-namespace :- s/Str
   gigabytes :- s/Int
   storage-class-name :- (s/maybe s/Str)]
  {:kind       "PersistentVolumeClaim",
   :apiVersion "v1",
   :metadata   {:name      pvc-name,
                :namespace pvc-namespace,
                :labels    {}
                }
   :spec       {:accessModes      ["ReadWriteOnce"],
                :resources        {:requests {:storage (str gigabytes "Gi")}},
                :storageClassName (or storage-class-name "default")}})
