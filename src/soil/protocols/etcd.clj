(ns soil.protocols.etcd)

(defprotocol Etcd
  (put! [this key value])
  (get! [this key])
  (get-prefix! [this key])
  (delete! [this key]))
