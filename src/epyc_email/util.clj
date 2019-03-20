(ns epyc-email.util
  (:require clojure.pprint))

(defn spy [data]
  (clojure.pprint/pprint data)
  data)

(defn now []
  (new java.util.Date))
