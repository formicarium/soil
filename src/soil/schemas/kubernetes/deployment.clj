(ns soil.schemas.kubernetes.deployment
  (:require [schema.core :as s]
            [soil.schemas.kubernetes.common :as c]
            [soil.schemas.kubernetes.pod :as pod]))

(def DeploymentStrategyType
  (s/enum "RollingUpdate" "Recreate"))

(def RollingUpdateDeployment
  {:maxSurge       (s/either s/Int s/Str),
   :maxUnavailable (s/either s/Int s/Str)})

(def DeploymentStrategy
  {(s/optional-key :rollingUpdate) RollingUpdateDeployment,
   :type                           DeploymentStrategyType})

(def DeploymentSpec
  {(s/optional-key :minReadySeconds)         s/Int,
   (s/optional-key :paused)                  s/Bool,
   (s/optional-key :progressDeadlineSeconds) s/Int,
   (s/optional-key :replicas)                s/Int,
   (s/optional-key :revisionHistoryLimit)    s/Int,
   (s/optional-key :selector)                (s/maybe c/LabelSelector),
   (s/optional-key :strategy)                DeploymentStrategy,
   :template                                 pod/PodTemplateSpec})

(def DeploymentCondition
  {:lastTransitionTime c/Time,
   :lastUpdateTime     c/Time,
   :message            s/Str,
   :reason             s/Str,
   :status             (s/enum "True" "False" "Unknown"),
   :type               s/Str})

(def DeploymentStatus
  {(s/optional-key :availableReplicas)   s/Int,
   (s/optional-key :collisionCount)      s/Int,
   :conditions                           [DeploymentCondition],
   (s/optional-key :observedGeneration)  s/Int,
   (s/optional-key :readyReplicas)       s/Int,
   (s/optional-key :replicas)            s/Int,
   (s/optional-key :unavailableReplicas) s/Int,
   (s/optional-key :updatedReplicas)     s/Int})

(def Deployment
  {:apiVersion                c/ApiVersion,
   :kind                      (s/eq "Deployment"),
   (s/optional-key :metadata) c/ObjectMeta,
   :spec                      DeploymentSpec,
   (s/optional-key :status)   DeploymentStatus})
