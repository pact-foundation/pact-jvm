(ns leiningen.pact-verify
  (:require [au.com.dius.pact.provider.lein.verify-provider :as verify]
            [clojure.string :as str])
  (:import (au.com.dius.pact.provider.lein LeinVerifierProxy)))

(defn- split-string [arg]
  (if (.contains arg "=")
    (let [key-val (str/split arg #"=")]
      [(read-string (first key-val)) (second key-val)])
    [(read-string arg) arg]))

(defn- parse-args [args]
  (if (not-empty args)
    (apply assoc {} (mapcat #(split-string %) args))
    {}))

(defn pact-verify
      "Verifies pact files against a provider"
      [project & args]
      (if (contains? project :pact)
        (let [verifier (LeinVerifierProxy. project (parse-args args))]
          (verify/verify verifier (-> project :pact)))
        (throw (ex-info "No pact definition was found in the project"))))
