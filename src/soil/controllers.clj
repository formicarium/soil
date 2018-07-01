(ns soil.controllers)


(defn get-namespace [namespace]
  (println "Getting namespace...")
  {:id "Teste"})

(defn get-namespaces []
  (println "Getting namespaces...")
  [{:id "Teste"}])


(defn create-namespace [namespace]
  (println "Creating namespace...")
  {:id   "id"
   :name "Created namespace"})


(defn delete-namespace [namespace]
  (println "Deleting namespace...")
  {:id   "id"
   :name "Deleted namesapce"})

(defn deploy [namespace name shard image]
  (println "Deploying...")
  {:namespace {:id   namespace
               :name namespace}
   :name      name
   :shard     shard
   :image     image})
