(ns soil.schema
  (:require [soil.resolvers :as resolvers]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]))


(def type-defs
  {:objects
   {:Service {:fields {:id        {:type '(non-null ID)}
                       :name      {:type '(non-null String)}
                       :shard     {:type 'String}
                       :namespace {:type :Namespace}}}
    :Namespace
             {:fields {:id   {:type '(non-null ID)}
                       :name {:type '(non-null String)}}}}
   :queries
   {:namespace
    {:type    :Namespace
     :resolve :namespace
     :args    {:id {:type '(non-null ID)}}}}


   :mutations
   {:createNamespace {:type    :Namespace
                      :args    {:name {:type '(non-null String)}}
                      :resolve :create-namespace}
    :deleteNamespace {:type    :Namespace
                      :args    {:id {:type '(non-null ID)}}
                      :resolve :delete-namespace}
    :deploy          {:type    :Service
                      :args    {:name      {:type '(non-null String)}
                                :shard     {:type '(non-null String)}
                                :image     {:type '(non-null String)}
                                :namespace {:type '(non-null String)}}
                      :resolve :deploy}}})

(def query-resolvers
  (into {}
        [[:namespace resolvers/get-namespace]
         [:namespaces resolvers/get-namespaces]]))

(def mutation-resolvers
  (into {}
        [[:create-namespace resolvers/create-namespace]
         [:delete-namespace resolvers/delete-namespace]
         [:deploy resolvers/deploy]]))

(def resolver-map (merge query-resolvers mutation-resolvers))

(defn load-schema
  []
  (-> type-defs
      (util/attach-resolvers resolver-map)
      schema/compile))