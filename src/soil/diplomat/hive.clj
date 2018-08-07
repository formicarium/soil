(ns soil.diplomat.hive
  (:require [soil.components.http-client.http-client :as http]))

(defn notify-service-deployed
  [devspace service-name]
  (http/post (str "http//hive." devspace ".cluster.local" "/service/" service-name "/deployed")
             {:service-name service-name
              :stinger-host (str "http://" service-name "-stinger")}))
