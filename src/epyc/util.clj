(ns epyc.util
  (:require clojure.pprint))

(defn spy [data]
  (clojure.pprint/pprint data)
  data)

(defn now []
  (new java.util.Date))

(defn uuid []
  (.toString (java.util.UUID/randomUUID)))
