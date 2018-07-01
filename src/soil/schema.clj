(ns soil.schema
  (:require [soil.resolvers :as resolvers]))


(def schema
  {:objects
   {:Namespace
    {:fields {:id   {:type '(non-null ID)}
              :name {:type '(non-null String)}}}}
   :queries
   {:namespace
    {:type :Namespace
     :args {:id {:type '(non-null ID)}}}}


   :mutations
   {:createNamespace {:args {:name {:type '(non-null String)}}}
    :deleteNamespace {:args {:id {:type '(non-null ID)}}}
    :deploy          {:args {:name      {:type '(non-null String)}
                             :shard     {:type '(non-null String)}
                             :image     {:type '(non-null String)}
                             :namespace {:type '(non-null String)}}}}})

(def query-resolvers
  (into {}
        [[:namespace resolvers/get-namespace]
         [:namespaces resolvers/get-namespaces]]))

(def mutation-resolvers
  (into {}
        [[:create-namespace resolvers/create-namespace
          :delete-namespace resolvers/delete-namespace
          :deploy resolvers/deploy]]))

(def resolvers (merge query-resolvers mutation-resolvers))