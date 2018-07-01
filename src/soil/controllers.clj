(ns soil.controllers)


(defn get-namespace [namespace]
  (println "Getting namespace..."
           {:name "Teste"}))

(defn get-namespaces []
  (println "Getting namespaces...")
  [{:name "Teste"}])


(defn create-namespace [namespace]
  (println "Creating namespace...")
  {:name "Created namespace"})


(defn delete-namespace [namespace]
  (println "Deleting namespace...")
  {:name "Deleted namesapce"})

(defn deploy [namespace service-name shard image]
  (println "Deploying...")
  {:namespace    {:name namespace}
   :service-name service-name
   :shard        shard
   :image        image})
