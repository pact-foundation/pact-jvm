(ns au.com.dius.pact.provider.lein.verify-provider
  (import (au.com.dius.pact.provider ProviderInfo ConsumerInfo))
  (:require [clojure.pprint :as pp]
            [clojure.java.io :as io]))

(defn to-provider [provider-info]
  (let [provider (ProviderInfo. (-> provider-info key str))
        provider-data (val provider-info)]
    (if (contains? provider-data :protocol) (.setProtocol provider (:protocol provider-data)))
    (if (contains? provider-data :host) (.setHost provider (:host provider-data)))
    (if (contains? provider-data :port) (.setPort provider (-> provider-data :port int)))
    (if (contains? provider-data :path) (.setPath provider (:path provider-data)))
    (if (contains? provider-data :insecure) (.setInsecure provider (:insecure provider-data)))
    (if (contains? provider-data :trust-store) (.setTrustStore provider (:trust-store provider-data)))
    (if (contains? provider-data :trust-store-password) (.setTrustStorePassword provider (:trust-store-password provider-data)))
    (if (contains? provider-data :state-change-url) (.setStateChangeUrl provider (:state-change-url provider-data)))
    (if (contains? provider-data :state-change-uses-body) (.setStateChangeUsesBody provider (:state-change-uses-body provider-data)))
    (if (contains? provider-data :verification-type) (.setVerificationType provider (:verification-type provider-data)))
    (if (contains? provider-data :packages-to-scan) (.setPackagesToScan provider (:packages-to-scan provider-data)))
    ;
    ;def startProviderTask
    ;def terminateProviderTask
    ;
    ;def requestFilter
    ;def stateChangeRequestFilter
    ;def createClient
    ;
    provider))

(defn to-consumer [consumer-info]
  (let [consumer (ConsumerInfo. (-> consumer-info key str))
        consumer-data (val consumer-info)]
    (if (contains? consumer-data :pact-file) (.setPactFile consumer (-> consumer-data :pact-file io/as-url)))
    (if (contains? consumer-data :state-change-url) (.setStateChange consumer (:state-change-url consumer-data)))
    (if (contains? consumer-data :state-change-uses-body) (.setStateChangeUsesBody consumer (:state-change-uses-body consumer-data)))
    (if (contains? consumer-data :verification-type) (.setVerificationType consumer (:verification-type consumer-data)))
    (if (contains? consumer-data :packages-to-scan) (.setPackagesToScan consumer (:packages-to-scan consumer-data)))
    consumer))

(defn verify-providers [verifier providers]
  (let [failures (mapcat #(let [provider (to-provider %)
                           consumers (->> % val :hasPactWith (map to-consumer))]
      (.setConsumers provider consumers)
      (.verifyProvider verifier provider)) providers)]
    (if (not-empty failures)
      (do
        (.displayFailures verifier failures)
        (throw (RuntimeException. (str "There were " (count failures) " pact failures")))))))
