(ns au.com.dius.pact.provider.lein.verify-provider-test
    (:require [clojure.test :refer :all]
              [au.com.dius.pact.provider.lein.verify-provider :as verify-provider]))

;(deftest validates-URLs-correctly
;   (is (true? (verify-provider/valid-url? "http://localhost:1234/")))
;   (is (true? (verify-provider/valid-url? "file:///some/path")))
;   (is (false? (verify-provider/valid-url? "/some/path")))
;   (is (false? (verify-provider/valid-url? "blah blah blah"))))
