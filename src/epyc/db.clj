(ns epyc.db
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]))

(defprotocol IDb
  (migrate-schema [this schema])
  (drop-data [this])
  (new-player [this player])
  (get-player [this id])
  #_(new-turn [this player])
  (get-turn [this player-id])
  #_(play-turn [this player photo text]))

(defrecord Db
    [spec]
  IDb
  (migrate-schema [{spec :spec} schema]
    (if (-> (jdbc/query
             spec
             [(str "SELECT COUNT(*) "
                   "FROM information_schema.tables "
                   "WHERE table_name IN"
                   "('turn', 'player')")])
            first
            :count
            zero?)
      (do (log/info "Migration: START")
          (jdbc/execute! spec [schema])
          (log/info "Migration: DONE"))
      (log/warn "Can't migrate if tables exist! Aborting.")))
  (drop-data [{spec :spec}]
    ;; Is there a simpler way to do multiple commands?
    (jdbc/execute! spec [(str/join ";"
                                   ["TRUNCATE TABLE player CASCADE"
                                    "TRUNCATE TABLE game CASCADE"
                                    "TRUNCATE TABLE turn CASCADE"])]))
  (new-player [{spec :spec}
               {:as player :keys [id first_name last_name]}]
    (log/info "Creating" id first_name last_name)
    (jdbc/insert! spec :player player))

  (get-player [{spec :spec} player-id]
    (jdbc/query spec ["SELECT * FROM player WHERE id = ?"] player-id))

  (get-turn [{spec :spec} player-id]
    nil))




