(ns au.com.dius.pact.provider.lein.verify-provider
  (:require [clojure.pprint :as pp]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [leiningen.core.main :as lein])
  (:import (au.com.dius.pact.provider ProviderInfo ConsumerInfo)
           (au.com.dius.pact.core.model UrlSource)))

(defn wrap-task [verifier task-name]
  #(lein/resolve-and-apply (.getProject verifier) [task-name]))

(defn to-provider [verifier provider-info]
  (let [provider (ProviderInfo. (-> provider-info key str))
        provider-data (val provider-info)]
    (if (contains? provider-data :protocol) (.setProtocol provider (:protocol provider-data)))

    (if (contains? provider-data :host)
      (if (fn? (eval (:host provider-data)))
        (.setHost provider (.wrap verifier (eval (:host provider-data))))
        (.setHost provider (:host provider-data))))

    (if (contains? provider-data :port) (.setPort provider (-> provider-data :port int)))
    (if (contains? provider-data :path) (.setPath provider (:path provider-data)))
    (if (contains? provider-data :insecure) (.setInsecure provider (:insecure provider-data)))
    (if (contains? provider-data :trust-store) (.setTrustStore provider (:trust-store provider-data)))
    (if (contains? provider-data :trust-store-password) (.setTrustStorePassword provider (:trust-store-password provider-data)))
    (if (contains? provider-data :state-change-url) (.setStateChangeUrl provider (-> provider-data :state-change-url io/as-url)))
    (if (contains? provider-data :state-change-uses-body) (.setStateChangeUsesBody provider (:state-change-uses-body provider-data)))
    (if (contains? provider-data :verification-type) (.setVerificationType provider (:verification-type provider-data)))
    (if (contains? provider-data :packages-to-scan) (.setPackagesToScan provider (:packages-to-scan provider-data)))

    (if (contains? provider-data :request-filter) (.setRequestFilter provider (.wrap verifier (eval (:request-filter provider-data)))))
    (if (contains? provider-data :state-change-request-filter) (.setStateChangeRequestFilter provider (.wrap verifier (eval (:state-change-request-filter provider-data)))))
    (if (contains? provider-data :create-client) (.setCreateClient provider (.wrap verifier (eval (:create-client provider-data)))))

    (if (contains? provider-data :start-provider-task) (.setStartProviderTask provider (wrap-task verifier (:start-provider-task provider-data))))
    (if (contains? provider-data :terminate-provider-task) (.setTerminateProviderTask provider (wrap-task verifier (:terminate-provider-task provider-data))))
    provider))

(defn to-consumer [consumer-info]
  (let [consumer (ConsumerInfo. (-> consumer-info key str))
        consumer-data (val consumer-info)]
    (if (contains? consumer-data :pact-file) (.setPactFile consumer (-> consumer-data :pact-file io/as-url)))
    (if (contains? consumer-data :pact-source) (.setPactSource consumer (-> consumer-data :pact-source UrlSource.)))
    (if (contains? consumer-data :state-change-url) (.setStateChange consumer (:state-change-url consumer-data)))
    (if (contains? consumer-data :state-change-uses-body) (.setStateChangeUsesBody consumer (:state-change-uses-body consumer-data)))
    (if (contains? consumer-data :verification-type) (.setVerificationType consumer (:verification-type consumer-data)))
    (if (contains? consumer-data :packages-to-scan) (.setPackagesToScan consumer (:packages-to-scan consumer-data)))
    consumer))

(defn execute-start-provider-task [provider]
  (if-let [start-task (.getStartProviderTask provider)]
    (do
      (lein/info (str "Executing start provider task for " (.getName provider)))
      (start-task))))

(defn execute-terminate-provider-task [provider]
  (if-let [terminate-task (.getTerminateProviderTask provider)]
    (do
      (lein/info (str "Executing terminate provider task for " (.getName provider)))
      (terminate-task))))

(defn verify-providers [verifier providers]
  (let [failures (mapcat #(let [provider (to-provider verifier %)
                                consumers (->> % val :has-pact-with (map to-consumer))]
                            (.setConsumers provider consumers)
                            (execute-start-provider-task provider)
                            (try
                              (.verifyProvider verifier provider)
                              (finally (execute-terminate-provider-task provider)))
                            ) providers)]
    (if (not-empty failures)
      (do
        (.displayFailures verifier failures)
        (throw (ex-info (str "There were " (count failures) " pact failures") {}))))))

(defn verify [verifier pact-info]
  (verify-providers verifier (:service-providers pact-info)))

(defn has-property? [property args]
  (contains? args property))

(defn get-property [property args]
  (args property))
