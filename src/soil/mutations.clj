(ns soil.mutations
  (:require [clojure.core.async :refer [<!!]]
            [kubernetes.api.v1 :as k8s]
            [soil.k8s :refer [ctx]]))

(defn list-namespaces []
  (<!! (k8s/list-namespaced-node ctx))
  )