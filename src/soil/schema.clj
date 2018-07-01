(ns soil.schema)


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
                    {:createNamespace {:args {:name {:type '(non-null String)}}}}
   :deleteNamespace {:args {:id {:type '(non-null ID)}}}
   :deploy          {:args {:name      {:type '(non-null String)}
                            :shard     {:type '(non-null String)}
                            :image     {:type '(non-null String)}
                            :namespace {:type '(non-null String)}}}
   })
