(ns soil.components.http-client.http-client
  (:require [com.stuartsierra.component :as component]
            [clj-http.client :as http]
            [cheshire.core :as cheshire]))



(defn post
  [url body]
  (http/post url {:body         (cheshire/generate-string body)
                  :content-type :json}))

(defrecord HttpClient []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn new-http-client []
  (map->HttpClient {}))
