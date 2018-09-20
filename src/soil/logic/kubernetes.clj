(ns soil.logic.kubernetes
  (:require [schema.core :as s]
            [clj-service.misc :as misc]))

(s/defn res->app-name :- (s/maybe s/Str)
  [res :- (s/pred map?)]
  (get-in res [:metadata :labels :formicarium.io/application]))

(s/defn find-by-app-name :- (s/maybe (s/pred map?))
  [app-name :- s/Str
   res :- [(s/pred map?)]]
  (misc/find-first res #(= app-name (res->app-name %))))

