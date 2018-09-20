(ns soil.components.etcd-mock
  (:require [com.stuartsierra.component :as component]
            [soil.protocols.etcd :as protocols.etcd]
            [clj-service.protocols.config :as protocols.config]
            [clj-service.exception :as exception]))



(defrecord EtcdMock [config mock]
  component/Lifecycle
  (start [this]
    (assoc this :mock (atom {})))
  (stop [this]
    (dissoc this :mock))

  protocols.etcd/Etcd
  (put! [this key value]
    (swap! mock (fn [m] (assoc m key value)))
    this)

  (get-maybe [this key]
    {:key key
     :value (get @mock key)})

  (get! [this key]
    (or (protocols.etcd/get-maybe this key)
        (exception/not-found! {:key-not-found key
                               :log           :etcd-get-error})))

  (get-prefix! [this key]
    (->> (select-keys @mock (->> @mock
                             keys
                             (filter (fn [k] (clojure.string/starts-with? k key)))))
         (mapv (fn [[k v]] {:key k :value v}))))

  (delete! [this key]
    (swap! mock (fn [m] (dissoc m key)))
    this)

  (delete-prefix! [this key]
    (swap! mock (fn [m] (apply dissoc (->> (mapv :key (protocols.etcd/get-prefix! this key))
                                      (concat [m])))))))

(defn new-etcd []
  (map->EtcdMock {}))
