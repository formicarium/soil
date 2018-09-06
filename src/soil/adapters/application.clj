(ns soil.adapters.application
  (:require [schema.core :as s]
            [soil.schemas.application :as schemas.application]
            [soil.models.application :as models.application]
            [clj-service.protocols.config :as protocols.config]
            [clj-service.exception :refer [server-error!]]
            [soil.logic.application :as logic.application]
            [soil.logic.interface :as logic.interface]
            [clj-json-patch.core :as json-patch]
            [clj-service.misc :as misc]))

(defn- deep-map-keys
  [f coll]
  (cond
    (map? coll) (misc/map-vals #(deep-map-keys f %) (misc/map-keys f coll))
    (vector? coll) (mapv #(deep-map-keys f %) coll)
    (list? coll) (map #(deep-map-keys f %) coll)
    :else coll))

(defn- patch [obj patches]
  (->> patches
       (mapv #(misc/map-keys name %))
       (json-patch/patch (deep-map-keys #(subs (str %) 1) obj))
       (deep-map-keys keyword)))

(s/defn definition->application :- models.application/Application
  [app-definition :- schemas.application/ApplicationDefinition
   config :- protocols.config/IConfig]
  (let [domain (protocols.config/get! config :domain)]
    #:application{:name       (:name app-definition)
                  :devspace   (:devspace app-definition)
                  :containers (mapv #(do #:container{:name      (:name %)
                                                     :image     (:image %)
                                                     :env       (:env %)
                                                     :syncable? (:syncable? %)}) (:containers app-definition))
                  :interfaces (mapv #(logic.interface/new
                                       (merge %
                                              {:devspace (:devspace app-definition)
                                               :service  (:name app-definition)
                                               :type     (keyword "interface.type" (name (:type %)))
                                               :domain   domain})) (:interfaces app-definition))
                  :status     :application.status/template
                  :patches    (:patches app-definition)}))

(s/defn application+container->container-ports :- [(s/pred map?)]
  [application :- models.application/Application
   container :- models.application/Container]
  (->> (logic.application/get-container-interfaces application container)
       (mapv #(do {:name          (:interface/name %)
                   :containerPort (:interface/port %)}))))

(s/defn application->containers :- [(s/pred map?)]
  [{:application/keys [containers] :as application} :- models.application/Application]
  (mapv (fn [container]
          {:name  (:container/name container)
           :image (:container/image container)
           :ports (application+container->container-ports application container)
           :env   (mapv #(do {:name  (name (key %))
                              :value (str (val %))}) (:container/env container))}) containers))

(s/defn application->deployment :- (s/pred map?)
  [{:application/keys [devspace] :as application} :- models.application/Application
   image-pull-secrets :- [s/Str]]
  (let [app-name (:application/name application)]
    (patch
      {:apiVersion "apps/v1"
       :kind       "Deployment"
       :metadata   {:name        app-name
                    :labels      {:app app-name}
                    :annotations {}
                    :namespace   devspace}
       :spec       {:selector {:matchLabels {:app app-name}}
                    :replicas 1
                    :template {:metadata {:labels      {:app app-name}
                                          :annotations {}
                                          :namespace   devspace}
                               :spec     {:hostname         app-name
                                          :containers       (application->containers application)
                                          :imagePullSecrets (mapv #(do {:name %}) (keep identity image-pull-secrets))}}}}
      (logic.application/get-deployment-patches application))))

(s/defn application+container->service-ports :- [(s/pred map?)]
  [application :- models.application/Application
   container :- models.application/Container]
  (->> (logic.application/get-container-interfaces application container)
       (mapv (fn [{:interface/keys [name port type]}]
               {:protocol   (if (= type :interface.type/udp) "UDP" "TCP")
                :name       name
                :port       (if (= name "default") 80 port)
                :targetPort name}))))

(s/defn application->service :- (s/pred map?)
  [application :- models.application/Application]
  (let [app-name (:application/name application)]
    (patch
      {:apiVersion "v1"
       :kind       "Service"
       :metadata   {:name        app-name
                    :labels      {:app app-name}
                    :annotations {}
                    :namespace   (:application/devspace application)}
       :spec       {:ports    (->> (:application/containers application)
                                   (mapv #(application+container->service-ports application %))
                                   (flatten))
                    :type     "NodePort"
                    :selector {:app app-name}}}
      (logic.application/get-service-patches application))))

(s/defn application+interface->ingress-rule :- (s/pred map?)
  [application :- models.application/Application
   {:interface/keys [name host]} :- models.application/Interface]
  {:host host
   :http {:paths [{:backend {:serviceName (:application/name application)
                             :servicePort name}
                   :path    "/"}]}})

(s/defn application->ingress :- (s/pred map?)
  [{:application/keys [interfaces] :as application} :- models.application/Application]
  (let [app-name (:application/name application)]
    (patch
      {:apiVersion "extensions/v1beta1"
       :kind       "Ingress"
       :metadata   {:name        app-name
                    :annotations {:kubernetes.io/ingress.class "nginx"}
                    :labels      {:app app-name}
                    :namespace   (:application/devspace application)}
       :spec       {:rules (->> interfaces
                                (filter logic.interface/http-like?)
                                (filter logic.interface/exposed?)
                                (mapv (partial application+interface->ingress-rule application)))}}
      (logic.application/get-ingress-patches application))))

(s/defn application->config-map :- (s/pred map?)
  [ports :- [s/Int]
   {:application/keys [devspace] :as application} :- models.application/Application]
  (let [tcp-interfaces (logic.application/get-tcp-like-interfaces application)]
    (if (>= (count ports)
            (count tcp-interfaces))
      {:data (->> tcp-interfaces
                  (mapv #(str devspace "/" (:application/name application) ":" (:interface/port %)))
                  (zipmap (map (comp keyword str) ports)))}
      (server-error! (ex-info "Not enough available ports"
                              {:ports          ports
                               :tcp-interfaces tcp-interfaces})))))

(s/defn application->urls :- schemas.application/ApplicationUrls
  [application :- models.application/Application]
  (->> (:application/interfaces application)
       (mapv (fn [{:interface/keys [name host type]}] {name (str (clojure.core/name type) "://" host)}))
       (apply merge)))

(s/defn internal->wire :- schemas.application/Application
  [application :- models.application/Application]
  {:name     (:application/name application)
   :devspace (:application/devspace application)
   :syncable (logic.application/syncable? application)
   :links    (application->urls application)})

(s/defn application-key :- s/Str
  [devspace :- s/Str
   app-name :- s/Str]
  (clojure.string/join "/" ["applications" devspace app-name]))

(s/defn application->key :- s/Str
  [{:application/keys [devspace name]} :- models.application/Application]
  (application-key devspace name))
