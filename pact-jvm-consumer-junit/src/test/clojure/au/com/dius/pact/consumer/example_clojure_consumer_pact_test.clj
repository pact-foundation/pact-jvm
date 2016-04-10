(ns au.com.dius.pact.consumer.example_clojure_consumer_pact_test
  (:require
    [org.httpkit.client :as http]
    [clojure.test :refer :all])
  (:import [au.com.dius.pact.consumer ConsumerPactBuilder ConsumerPactTest TestRun]
           [au.com.dius.pact.model MockProviderConfig$]))

(deftest example-clojure-consumer-pact-test
  (let [consumer-fragment (-> "clojure_test_consumer"
                            ConsumerPactBuilder/consumer
                            (.hasPactWith "test_provider")
                            (.uponReceiving "clojure test interaction")
                            (.path "/sample")
                            (.method "GET")
                            .willRespondWith
                            (.status 200)
                            .toFragment)
        config (-> MockProviderConfig$/MODULE$ (.createDefault))]
    (is (= (ConsumerPactTest/PACT_VERIFIED)
        (.runConsumer consumer-fragment config
          (proxy [TestRun] []
            (run [_] (
               #(is (= 200
                   (:status
                     @(http/get (str (.url config) "/sample")))))))))))))
