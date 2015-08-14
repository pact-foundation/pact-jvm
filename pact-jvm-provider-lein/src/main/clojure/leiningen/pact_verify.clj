(ns leiningen.pact-verify
    (:require [clojure.pprint :as pprint]))

(defn pact-verify
      "Verifies pact files against a provider"
      [project & args]
      (pprint/pprint "pact-verify called"))
