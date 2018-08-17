(ns soil.components.etcd
  (:require [com.stuartsierra.component :as component]
            [soil.protocols.etcd :as protocols.etcd]
            [clj-service.protocols.config :as protocols.config])
  (:import (com.ibm.etcd.client EtcdClient)
           (com.google.protobuf ByteString)
           (com.ibm.etcd.client.kv EtcdKvClient KvClient$FluentPutRequest KvClient$FluentRangeRequest)
           (java.util Base64)
           (com.ibm.etcd.api KeyValue RangeResponse)))

(defn ^:private str->byte-string ^ByteString [s]
  (ByteString/copyFromUtf8 s))

(defn ^:private byte-string->str [^ByteString bs]
  (-> bs .toStringUtf8))

(defn ^:private encode-base64 [s]
  (String. (.encode (Base64/getEncoder) (.getBytes s))))

(defn ^:private decode-base64 [b64]
  (String. (.decode (Base64/getDecoder) (.getBytes b64))))

(defn ^:private clj->base64 [m]
  (-> m pr-str encode-base64))

(defn ^:private base64->clj [b64]
  (-> b64 decode-base64 read-string))

(defn ^:private put-string [^EtcdKvClient kv-client k v]
  (let [pk ^ByteString (str->byte-string (name k))
        pv ^ByteString (str->byte-string v)]
    (-> kv-client
        (.put pk pv)
        (.sync))))

(defn ^:private with-prefix [^KvClient$FluentRangeRequest kv prefix?]
  (if prefix?
    (.asPrefix kv)
    kv))

(defn ^:private get-key [^EtcdKvClient kv-client k prefix?]
  (let [response ^RangeResponse (-> (.get kv-client (str->byte-string (name k)))
                                    (with-prefix prefix?)
                                    .sync)]
    (mapv #(do {:key   (-> % .getKey byte-string->str keyword)
                :value (-> % .getValue byte-string->str)})
      (into [] (.getKvsList response)))))

(defn ^:private delete-key [^EtcdKvClient kv-client k]
  (.sync (.delete kv-client (str->byte-string (name k)))))

(defn ^:private delete-prefix-key [^EtcdKvClient kv-client k]
  (.sync (.asPrefix (.delete kv-client (str->byte-string (name k))))))

(defn ^:private get-clj [^EtcdKvClient kv-client k prefix?]
  (mapv #(update % :value base64->clj) (get-key kv-client k prefix?)))

(defn ^:private put-clj [^EtcdKvClient kv-client k m]
  (put-string kv-client k (clj->base64 m)))

(defrecord Etcd [config]
  component/Lifecycle
  (start [this]
    (let [{:keys [port endpoint]} (protocols.config/get! config :etcd)
          etcd-client (-> (EtcdClient/forEndpoint endpoint port) .withPlainText .build)
          kv-client   (.getKvClient etcd-client)]
      (assoc this :etcd-client etcd-client
                  :kv-client kv-client)))
  (stop [this]
    (.close (:kv-client this))
    (.close (:etcd-client this))
    (dissoc :etcd-client :kv-client))

  protocols.etcd/Etcd
  (put! [this key value]
    (put-clj (:kv-client this) key value))

  (get! [this key]
    (first (get-clj (:kv-client this) key false)))

  (get-prefix! [this key]
    (get-clj (:kv-client this) key true))

  (delete! [this key]
    (delete-key (:kv-client this) key))

  (delete-prefix! [this key]
    (delete-prefix-key (:kv-client this) key)))

(defn new-etcd []
  (map->Etcd {}))
