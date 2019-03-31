(ns leiningen.pact-verify-test
    (:require [clojure.test :refer :all]
              [leiningen.pact-verify :as pact-verify]))

(deftest fails-if-there-is-no-pact-definition-in-project
   (is (thrown? RuntimeException
          (pact-verify/pact-verify {}))))

