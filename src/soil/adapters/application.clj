(ns soil.adapters.application
  (:require [schema.core :as s]
            [soil.schemas.application :as schemas.application]
            [soil.schemas.kubernetes.deployment :as schemas.k8s.deployment]
            [soil.schemas.kubernetes.service :as schemas.k8s.service]
            [soil.schemas.kubernetes.ingress :as schemas.k8s.ingress]
            [soil.models.application :as models.application]
            [clj-service.protocols.config :as protocols.config]
            [clj-service.exception :refer [server-error!]]
            [soil.logic.application :as logic.application]
            [soil.logic.interface :as logic.interface]
            [clj-json-patch.core :as json-patch]
            [clj-service.misc :as misc]
            [clj-service.adapt :as adapt]))

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
  (let [app-name            (:application/name application)
        syncable-containers (set (map :container/name (filter #(true? (:container/syncable? %))
                                                        (:application/containers application))))
        patches             (logic.application/get-deployment-patches application)]
    (patch
      {:apiVersion "apps/v1"
       :kind       "Deployment"
       :metadata   {:name        app-name
                    :labels      {:formicarium.io/application         app-name
                                  :formicarium.io/patches             (adapt/to-edn patches)
                                  :formicarium.io/syncable-containers (adapt/to-edn syncable-containers)}
                    :annotations {}
                    :namespace   devspace}
       :spec       {:selector {:matchLabels {:formicarium.io/application app-name}}
                    :replicas 1
                    :template {:metadata {:labels      {:formicarium.io/application app-name}
                                          :annotations {}
                                          :namespace   devspace}
                               :spec     {:hostname         app-name
                                          :containers       (application->containers application)
                                          :imagePullSecrets (mapv #(do {:name %}) (keep identity image-pull-secrets))}}}}
      patches)))

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
  (let [app-name   (:application/name application)
        port-types (into {} (map #(do [(:interface/name %) (:interface/type %)]) (:application/interfaces application)))
        patches    (logic.application/get-service-patches application)]
    (patch
      {:apiVersion "v1"
       :kind       "Service"
       :metadata   {:name        app-name
                    :labels      {:formicarium.io/application app-name
                                  :formicarium.io/port-types  (adapt/to-edn port-types)
                                  :formicarium.io/patches     (adapt/to-edn patches)}
                    :annotations {}
                    :namespace   (:application/devspace application)}
       :spec       {:ports    (->> (:application/containers application)
                                   (mapv #(application+container->service-ports application %))
                                   (flatten))
                    :type     "NodePort"
                    :selector {:formicarium.io/application app-name}}}
      patches)))

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
                    :labels      {:formicarium.io/application app-name}
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

;; ==================================================

(s/defn edn-label->clj [k8s-res label] (-> k8s-res :metadata :labels label adapt/from-edn))

(s/defn service->port-type [service interface-name]
  (get (edn-label->clj service :formicarium.io/port-types) interface-name))

(s/defn ingress->interface-host [ingress interface-name]
  (->> ingress :spec :rules
       (filter
         #(first
            (->> % :http :paths
                 (filter (fn [v]
                           (= (-> v :backend :servicePort)
                             interface-name)))))) first :host))

(s/defn k8s->interfaces :- [models.application/Interface]
  [deployment service ingress]
  (mapcat
    (fn [container]
      (map #(do #:interface{:container (:name container)
                            :name      (:name %)
                            :type      (service->port-type service (:name %))
                            :host      (ingress->interface-host ingress (:name %))
                            :port      (:containerPort %)}) (:ports container)))
    (-> deployment :spec :template :spec :containers)))

(s/defn k8s-container->envs :- {s/Str s/Str}
  [container]
  (->> (:env container)
       (map #(do [(:name %) (:value %)]))
       (into {})))

(s/defn k8s->containers :- [models.application/Container]
  [deployment]
  (map (fn [container]
         #:container{:name      (:name container)
                     :image     (:image container)
                     :syncable? ((edn-label->clj deployment :formicarium.io/syncable-containers) (:name container))
                     :env       (k8s-container->envs container)})
    (-> deployment :spec :template :spec :containers)))

(s/defn k8s->patches :- [models.application/EntityPatch]
  [deployment service ingress]
  (concat (edn-label->clj deployment :formicarium.io/patches)
          (edn-label->clj service :formicarium.io/patches)
          (edn-label->clj ingress :formicarium.io/patches)))

(s/defn k8s->application :- models.application/Application
  [deployment :- schemas.k8s.deployment/Deployment
   service :- schemas.k8s.service/Service
   ingress :- schemas.k8s.ingress/Ingress]
  #:application{:name       (-> deployment :metadata :labels :formicarium.io/application)
                :devspace   (-> deployment :metadata :namespace)
                :interfaces (vec (k8s->interfaces deployment service ingress))
                :containers (vec (k8s->containers deployment))
                :patches    (vec (k8s->patches deployment service ingress))})
