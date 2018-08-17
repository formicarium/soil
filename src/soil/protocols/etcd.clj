(ns soil.protocols.etcd)

(defprotocol Etcd
  (put! [this key value])
  (get-maybe [this key])
  (get! [this key])
  (get-prefix! [this key])
  (delete! [this key])
  (delete-prefix! [this key]))

(def IEtcd (:on-interface Etcd))