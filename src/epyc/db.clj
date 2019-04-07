(ns epyc.db
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [epyc.util :refer [spy]]))

(defprotocol IDb
  (migrate-schema [this schema])
  (drop-data [this])
  (new-player [this player])
  (get-player [this id])
  (new-turn [this game-id player-id])
  (get-turn [this player-id])
  #_(play-turn [this player photo text])
  (new-game [this player-id]
    "Creates new turn in new game for player")
  (get-game [this game-id]))

(defn ^:private db-turn->turn
  [{:keys [t_id p_id g_id status text_turn preceding]}]
  (merge {:id         t_id
          :player-id  p_id
          :game-id    g_id
          :status     status
          :text-turn? text_turn}
         (when preceding
           {:preceding preceding})))

(defn ^:private get-preceding-turn
  [spec turn]
  (let [turns (:turns (get-game spec (:game-id turn)))]
    (last turns)))

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
      (do (log/info "DB: Migrating")
          (jdbc/execute! spec [schema]))
      (log/warn "Can't migrate if tables exist! Aborting.")))

  (drop-data [{spec :spec}]
    (log/info "DB: Truncating tables.")
    ;; Is there a simpler way to do multiple commands?
    (jdbc/execute! spec [(str/join ";"
                                   ["DROP TABLE player CASCADE"
                                    "DROP TABLE game CASCADE"
                                    "DROP TABLE turn CASCADE"])]))

  (new-player [{spec :spec}
               {:keys [id first_name last_name]}]
    (log/info "Creating" id first_name last_name)
    (jdbc/insert! spec :player {:p_id       id
                                :first_name first_name
                                :last_name  last_name}))


  (get-player [{spec :spec} player-id]
    (log/info "Getting player" player-id)
    (first (jdbc/query spec ["SELECT p_id as id, first_name, last_name FROM player WHERE p_id = ?" player-id])))

  (new-game [{spec :spec} player-id]
    (log/info "Creating game for" player-id)
    (->> {:status "active"}
         (jdbc/insert! spec :game)
         first
         :g_id))

  ;; TODO: account for game status
  (get-game [{spec :spec} game-id]
    (log/info "Getting game" game-id)
    {:id    game-id
     :turns (mapv db-turn->turn
                  (jdbc/query spec [(str "SELECT t_id, p_id, g_id, "
                                         "status, text_turn "
                                         "FROM turn WHERE g_id = ?")
                                    game-id]))})

  (new-turn [{spec :spec} game-id player-id]
    (log/info "Creating turn in game" game-id "for" player-id)
    (some->> {:p_id      player-id
              :g_id      game-id
              :text_turn true ;; TODO
              }
             (jdbc/insert! spec :turn)
             first
             db-turn->turn))

  (get-turn [{spec :spec} player-id]
    (log/info "Getting turn for" player-id)
    (some-> (jdbc/query spec [(str "SELECT t_id, p_id, g_id, "
                                   "status, text_turn "
                                   "FROM turn WHERE p_id = ? "
                                   "AND status = 'unplayed'")
                              player-id])
            first
            db-turn->turn)))




