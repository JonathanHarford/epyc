(ns epyc.db
  (:require [clojure.java.jdbc :as jdbc]))

#_(defprotocol IDb
  (migrate-schema [this schema])
  (drop-data [this])
  (new-player [this id first-name last-name])
  (get-player [this id])
  (new-turn [this player])
  (get-turn [this player])
  (play-turn [this player photo text]))

#_(defrecord Db
    [db-spec]
  IDb
  (migrate-schema [this schema] nil)
  (drop-data [this] nil)
  (new-player [this id first-name last-name] nil)
  (get-player [this id] nil)
  (new-turn [this player] nil)
  (get-turn [this player] nil)
  (play-turn [this player photo text] nil))




