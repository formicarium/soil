(ns logic.devspace-test
  (:require [clojure.test :refer :all]
            [soil.logic.devspace :as l-env]))

(deftest namespace->devspace-test
  (testing "should be able to transform a kubernetes's namespace on a formicarium's devspace"
    (is (= {:name "carlos-rodrigues"}
           (l-env/namespace->devspace {:apiVersion "v1"
                                       :kind       "Namespace"
                                       :metadata   {:creationTimestamp "2018-07-03T04:54:49Z"
                                                    :name              "carlos-rodrigues"
                                                    :namespace         ""
                                                    :resourceRevision  "261"
                                                    :selfLink          "/api/v1/namespaces/carlos-rodrigues"
                                                    :uid               "36cddf76-7e7d-11e8-ac33-0800274d6e6b"}
                                       :spec       {:finalizers ["kubernetes"]}
                                       :status     {:phase "Active"}})))))
