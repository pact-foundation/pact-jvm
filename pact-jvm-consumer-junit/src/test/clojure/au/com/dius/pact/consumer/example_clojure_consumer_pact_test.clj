(ns au.com.dius.pact.consumer.example_clojure_consumer_pact_test
  (:require
    [org.httpkit.client :as http]
    [clojure.test :refer :all])
  (:import [au.com.dius.pact.consumer ConsumerPactBuilder ConsumerPactRunnerKt PactTestRun PactVerificationResult$Ok]
           [au.com.dius.pact.model MockProviderConfig]))

(deftest example-clojure-consumer-pact-test
  (let [consumer-pact (-> "clojure_test_consumer"
                            ConsumerPactBuilder/consumer
                            (.hasPactWith "test_provider")
                            (.uponReceiving "clojure test interaction")
                            (.path "/sample")
                            (.method "GET")
                            .willRespondWith
                            (.status 200)
                            .toPact)
        config (-> (MockProviderConfig/createDefault))]
    (is (= (PactVerificationResult$Ok/INSTANCE)
        (ConsumerPactRunnerKt/runConsumerTest consumer-pact config
          (proxy [PactTestRun] []
            (run [_] (
               #(is (= 200
                   (:status
                     @(http/get (str (.url config) "/sample")))))))))))))
