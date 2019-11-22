(ns logic.application-test
  (:require [clojure.test :refer :all]
            [soil.logic.application :as logic.application]
            [schema.core :as s]))

(deftest syncable-codes-test
  (s/without-fn-validation
    (is (= #{{:syncable-code/name "kratos"}
             {:syncable-code/name "liberty-lib"}}
           (logic.application/syncable-codes {:application/containers [{:container/syncable-codes #{{:syncable-code/name "kratos"}
                                                                                                    {:syncable-code/name "liberty-lib"}}}]})))
    (is (nil? (logic.application/syncable-codes {:application/containers [{:container/syncable-codes #{}}]})))))
