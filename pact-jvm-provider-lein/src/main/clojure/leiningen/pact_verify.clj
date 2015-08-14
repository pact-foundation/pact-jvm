(ns leiningen.pact-verify
    (:require [clojure.pprint :as pprint]))

(defn- verify-provider [provider consumers]
       (pprint/pprint provider)
       (pprint/pprint consumers))

(defn pact-verify
      "Verifies pact files against a provider"
      [project & args]
      (if (contains? project :pact)
        (doseq [provider (-> project :pact :serviceProviders)]
               (apply verify-provider provider))
        (throw (RuntimeException. "No pact definition was found in the project"))))

