(ns soil.schemas.kubernetes.common
  (:require [schema.core :as s]))

(def ApiVersion
  (s/enum "v1" "apps/v1"))

(def Time s/Str)


(def LocalObjectReference
  "https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names"
  {:name s/Str})

(def ConfigMapKeySelector
  {:key                       s/Str,
   :name                      s/Str,
   (s/optional-key :optional) s/Bool})


(def ObjectFieldSelector
  {:apiVersion s/Str
   :fieldPath  s/Str})

(def SecretKeySelector
  {:key                       s/Str,
   :name                      s/Str,
   (s/optional-key :optional) s/Bool})

(def ObjectMeta
  {(s/optional-key :labels)                     {s/Str s/Str},
   (s/optional-key :clusterName)                s/Str,
   (s/optional-key :generation)                 (s/maybe s/Int),
   (s/optional-key :creationTimestamp)          Time,
   (s/optional-key :uid)                        s/Str,
   (s/optional-key :name)                       s/Str,
   (s/optional-key :resourceVersion)            s/Str,
   (s/optional-key :selfLink)                   s/Str,
   (s/optional-key :deletionTimestamp)          Time,
   (s/optional-key :deletionGracePeriodSeconds) s/Int,
   (s/optional-key :annotations)                {s/Str s/Str},
   (s/optional-key :namespace)                  s/Str,
   (s/optional-key :generateName)               s/Str})

(def LabelOperator
  (s/enum "In" "NotIn" "Exists" "DoesNotExist"))

(def LabelSelectorRequirement
  {:key      s/Str,
   :operator LabelOperator,
   :values   [s/Str]})

(def LabelSelector
  {(s/optional-key :matchExpressions) [LabelSelectorRequirement],
   (s/optional-key :matchLabels)      {s/Str s/Str}})
