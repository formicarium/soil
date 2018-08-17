(ns soil.schemas.kubernetes.pod
  (:require [schema.core :as s]
            [soil.schemas.kubernetes.common :as c]))

(def HttpGet
  {(s/optional-key :host)        s/Str
   (s/optional-key :httpHeaders) [{:name  s/Str
                                   :value s/Str}]
   :path                         s/Str
   (s/optional-key :port)        (s/either s/Str s/Int)
   (s/optional-key :scheme)      s/Str})

(def ExecAction
  {:command [s/Str]})

(def Probe
  {(s/optional-key :exec)                ExecAction
   (s/optional-key :failureThreshold)    s/Int
   (s/optional-key :httpGet)             HttpGet
   (s/optional-key :initialDelaySeconds) s/Int
   (s/optional-key :periodSeconds)       s/Int
   (s/optional-key :successThreshold)    s/Int
   (s/optional-key :timeoutSeconds)      s/Int})

(def VolumeMount
  {(s/optional-key :mountPath)        s/Str,
   (s/optional-key :mountPropagation) s/Str,
   (s/optional-key :name)             s/Str,
   (s/optional-key :readOnly)         s/Bool,
   (s/optional-key :subPath)          s/Str})

(def VolumeDevice
  {:devicePath s/Str
   :name       s/Str})

(def EnvVarSource
  {(s/optional-key :configMapKeyRef)  c/ConfigMapKeySelector,
   (s/optional-key :fieldRef)         c/ObjectFieldSelector,
   (s/optional-key :resourceFieldRef) s/Any,
   (s/optional-key :secretKeyRef)     c/SecretKeySelector})

(def EnvVar
  {:name                       s/Str,
   (s/optional-key :value)     s/Str,
   (s/optional-key :valueFrom) EnvVarSource})

(def ContainerPort
  {(s/optional-key :containerPort) s/Int,
   (s/optional-key :hostIP)        s/Str,
   (s/optional-key :hostPort)      s/Int,
   (s/optional-key :name)          s/Str})

(def Container
  {(s/optional-key :terminationMessagePolicy) (s/enum "File" "FallbackToLogsOnError"),
   (s/optional-key :args)                     [s/Str],
   (s/optional-key :volumeMounts)             [VolumeMount],
   (s/optional-key :readinessProbe)           Probe,
   :name                                      s/Str,
   (s/optional-key :volumeDevices)            [VolumeDevice],
   (s/optional-key :command)                  [s/Str],
   (s/optional-key :env)                      [EnvVar],
   (s/optional-key :ports)                    [ContainerPort],
   (s/optional-key :livenessProbe)            (s/maybe Probe),
   (s/optional-key :terminationMessagePath)   s/Str,
   (s/optional-key :workingDir)               s/Str,
   (s/optional-key :imagePullPolicy)          (s/enum "Always" "Never" "IfNotPresent"),
   (s/optional-key :stdinOnce)                (s/maybe s/Bool),
   :image                                     s/Str})

(def SecretVolumeSource
  {:secretName                s/Str
   (s/optional-key :optional) s/Bool})

(def ConfigMapVolumeSource
  {:name s/Str})

(def Volume
  {:name                       s/Str
   (s/optional-key :secret)    SecretVolumeSource
   (s/optional-key :configMap) ConfigMapVolumeSource})

(def PodSpec
  {(s/optional-key :serviceAccount)                s/Str,
   (s/optional-key :hostPID)                       s/Bool,
   (s/optional-key :imagePullSecrets)              [c/LocalObjectReference],
   (s/optional-key :automountServiceAccountToken)  s/Bool,
   (s/optional-key :nodeSelector)                  {s/Str s/Str},
   (s/optional-key :hostNetwork)                   s/Bool,
   (s/optional-key :activeDeadlineSeconds)         s/Int,
   (s/optional-key :priorityClassName)             s/Any,
   (s/optional-key :hostname)                      s/Any,
   :containers                                     [Container],
   (s/optional-key :shareProcessNamespace)         s/Bool,
   (s/optional-key :volumes)                       [Volume],
   (s/optional-key :subdomain)                     s/Any,
   (s/optional-key :schedulerName)                 s/Any,
   (s/optional-key :priority)                      s/Int,
   (s/optional-key :nodeName)                      s/Any,
   (s/optional-key :restartPolicy)                 s/Any,
   (s/optional-key :dnsPolicy)                     s/Any,
   (s/optional-key :initContainers)                [Container],
   (s/optional-key :serviceAccountName)            s/Any,
   (s/optional-key :hostIPC)                       s/Bool,
   (s/optional-key :terminationGracePeriodSeconds) s/Int})

(def PodTemplateSpec
  {(s/optional-key :metadata) c/ObjectMeta,
   :spec                      PodSpec})
