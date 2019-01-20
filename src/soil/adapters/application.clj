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
            [clj-service.adapt :as adapt]
            [io.pedestal.log :as log]))

(defn- deep-map-keys
  [f coll skip-keys]
  (cond
    (map? coll) (into {} (map (fn [[k v]]
                                (if (skip-keys k)
                                  [(f k) v]
                                  [(f k) (deep-map-keys f v skip-keys)]))) coll)
    (vector? coll) (mapv #(deep-map-keys f % skip-keys) coll)
    (list? coll) (map #(deep-map-keys f % skip-keys) coll)
    :else coll))

(defn- patch [obj patches]
  (deep-map-keys keyword (->> patches
                              (mapv #(misc/map-keys name %))
                              (json-patch/patch (deep-map-keys #(subs (str %) 1) obj #{:labels :matchLabels :annotations :selector})))
                 #{"labels" "annotations" "matchLabels" "selector"}))

(s/defn definition+devspace->application :- models.application/Application
  [app-definition :- schemas.application/ApplicationDefinition
   devspace :- s/Str
   args :- (s/pred map?)
   config :- protocols.config/IConfig]
  (let [domain (protocols.config/get! config :domain)
        devspace-name (or devspace (:devspace app-definition))]
    #:application{:name       (:name app-definition)
                  :devspace   devspace-name
                  :args       args
                  :containers (mapv #(do #:container{:name      (:name %)
                                                     :image     (:image %)
                                                     :env       (:env %)
                                                     :syncable? (:syncable? %)}) (:containers app-definition))
                  :interfaces (mapv #(logic.interface/new
                                       (merge %
                                              {:devspace devspace-name
                                               :service  (:name app-definition)
                                               :type     (keyword "interface.type" (name (:type %)))
                                               :domain   domain})) (:interfaces app-definition))
                  :status     :application.status/template
                  :patches    (:patches app-definition)}))


(s/defn definition->application :- models.application/Application
  [app-definition :- schemas.application/ApplicationDefinition
   args :- (s/pred map?)
   config :- protocols.config/IConfig]
  (definition+devspace->application app-definition (:devspace app-definition) args config))

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
  (let [app-name (:application/name application)
        syncable-containers (set (map :container/name (filter #(true? (:container/syncable? %))
                                                              (:application/containers application))))
        patches (logic.application/get-deployment-patches application)]
    (patch
      {:apiVersion "apps/v1"
       :kind       "Deployment"
       :metadata   {:name        app-name
                    :labels      {"formicarium.io/application" app-name}
                    :annotations {"formicarium.io/patches"             (adapt/to-edn patches)
                                  "formicarium.io/syncable-containers" (adapt/to-edn syncable-containers)
                                  "formicarium.io/args"                (adapt/to-edn {})}
                    :namespace   devspace}
       :spec       {:selector {:matchLabels {"formicarium.io/application" app-name}}
                    :replicas 1
                    :template {:metadata {:labels      {"formicarium.io/application" app-name}
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
                :port       port
                :targetPort name}))))

(s/defn application->service :- (s/pred map?)
  [application :- models.application/Application]
  (let [app-name (:application/name application)
        port-types (into {} (map #(do [(:interface/name %) (:interface/type %)]) (:application/interfaces application)))
        patches (logic.application/get-service-patches application)]
    (patch
      {:apiVersion "v1"
       :kind       "Service"
       :metadata   {:name        app-name
                    :labels      {"formicarium.io/application" app-name}
                    :annotations {"formicarium.io/port-types" (adapt/to-edn port-types)
                                  "formicarium.io/patches"    (adapt/to-edn patches)}
                    :namespace   (:application/devspace application)}
       :spec       {:ports    (->> (:application/containers application)
                                   (mapv #(application+container->service-ports application %))
                                   flatten)
                    :type     "NodePort"
                    :selector {"formicarium.io/application" app-name}}}
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
  (let [app-name (:application/name application)
        patches (logic.application/get-ingress-patches application)]
    (patch
      {:apiVersion "extensions/v1beta1"
       :kind       "Ingress"
       :metadata   {:name        app-name
                    :annotations {"kubernetes.io/ingress.class" "nginx"
                                  "formicarium.io/patches"      (adapt/to-edn patches)}
                    :labels      {"formicarium.io/application" app-name}
                    :namespace   (:application/devspace application)}
       :spec       {:rules (->> interfaces
                                (filter logic.interface/http-like?)
                                (filter logic.interface/exposed?)
                                (mapv (partial application+interface->ingress-rule application)))}}
      (logic.application/get-ingress-patches application))))

(s/defn application->urls :- schemas.application/ApplicationUrls
  [application :- models.application/Application]
  (log/info :application application)
  (->> (:application/interfaces application)
       (mapv (fn [{:interface/keys [name host type]}] {(keyword name) (str (clojure.core/name type) "://" host)}))
       (apply merge)))

(s/defn internal->wire :- schemas.application/Application
  [application :- models.application/Application]
  {:name     (:application/name application)
   :devspace (:application/devspace application)
   :syncable (logic.application/syncable? application)
   :links    (application->urls application)})

;; ==================================================

(s/defn edn-annotation->clj [k8s-res annotation]
  (-> k8s-res :metadata :annotations (get annotation) adapt/from-edn))

(s/defn service->port-type [service interface-name]
  (get (edn-annotation->clj service "formicarium.io/port-types") interface-name))

(s/defn ingress->interface-host [ingress interface-name]
  (->> ingress :spec :rules
       (filter
         #(first
            (->> % :http :paths
                 (filter (fn [v]
                           (= (-> v :backend :servicePort)
                              interface-name)))))) first :host))

(defn render-host [interface service ingress node]
  (assoc interface :interface/host (if (logic.interface/tcp-like? interface)
                                     (str (logic.interface/get-node-ip node) ":" (-> service
                                                                                     :spec
                                                                                     :ports
                                                                                     (misc/find-first #(= (:name %) (:interface/name interface)))
                                                                                     :nodePort))
                                     (ingress->interface-host ingress (:interface/name interface)))))

(s/defn k8s->interfaces :- [models.application/Interface]
  [deployment service ingress node]
  (mapcat
    (fn [container]
      (map #(render-host #:interface{:container (:name container)
                                     :name      (:name %)
                                     :host      ""
                                     :type      (service->port-type service (:name %))
                                     :port      (:containerPort %)} service ingress node)
           (:ports container)))
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
                     :syncable? (contains?
                                  (edn-annotation->clj deployment "formicarium.io/syncable-containers")
                                  (:name container))
                     :env       (k8s-container->envs container)})
       (-> deployment :spec :template :spec :containers)))

(s/defn k8s->patches :- [models.application/EntityPatch]
  [deployment service ingress]
  (concat (edn-annotation->clj deployment "formicarium.io/patches")
          (edn-annotation->clj service "formicarium.io/patches")
          (when ingress (edn-annotation->clj ingress "formicarium.io/patches"))))

(s/defn k8s->application :- models.application/Application
  [deployment :- schemas.k8s.deployment/Deployment
   service :- schemas.k8s.service/Service
   ingress :- schemas.k8s.ingress/Ingress
   node :- s/Any]
  #:application{:name       (-> deployment :metadata :labels (get "formicarium.io/application"))
                :devspace   (-> deployment :metadata :namespace)
                :interfaces (vec (k8s->interfaces deployment service ingress node))
                :containers (vec (k8s->containers deployment))
                :patches    (vec (k8s->patches deployment service ingress))})

