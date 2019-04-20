(ns epyc.db
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]))

(defn ^:private db-turn->turn
  [{:keys [t_id p_id g_id m_id t_status text_turn content]}]
  (merge {:id         t_id
          :player-id  p_id
          :game-id    g_id
          :message-id m_id
          :status     t_status
          :text-turn? text_turn
          :content    content}))

(defn ^:private sql [& strs]
  (str/join " " strs))

(defn ^:private get-last-turn [{turns :turns}]
  (last turns))

(defn log [msg-str & args]
  (let [[player-id game-id turn-id] args]
    (log/info
     (str (when player-id (str "P" player-id))
          (when game-id (str " [" game-id (when turn-id (str "/" turn-id)) "]")) " " msg-str))))

(defn migrate-schema [dbspec schema]
  (if (-> (jdbc/query
           dbspec
           [(sql "SELECT COUNT(*) "
                 "FROM information_schema.tables "
                 "WHERE table_name IN"
                 "('turn', 'player')")])
          first
          :count
          zero?)
    (do (log/info "DB: Migrating")
        (jdbc/execute! dbspec [schema]))
    (log/warn "Can't migrate if tables exist! Aborting.")))

(defn drop-data
  [dbspec]
  (log/info "DB: Truncating tables.")
  ;; Is there a simpler way to do multiple commands?
  (jdbc/execute! dbspec
                 [(str/join
                   ";"
                   ["DROP TABLE player CASCADE"
                    "DROP TABLE game CASCADE"
                    "DROP TABLE turn CASCADE"])]))

(defn new-player
  [dbspec {:keys [id first_name last_name]}]
  (log "Creating" id)
  (jdbc/insert! dbspec
                :player
                {:p_id       id
                 :first_name first_name
                 :last_name  last_name}))


(defn get-player
  [dbspec player-id]
  (->> [(sql "SELECT p_id as id, first_name, last_name"
             "FROM player WHERE p_id = ?") player-id]
       (jdbc/query dbspec)
       first))

(defn new-game
  [dbspec player-id]
  (let [game-id (->> {:status "available"}
                     (jdbc/insert! dbspec :game)
                     first
                     :g_id)]
    (log "Created game" player-id game-id)
    game-id))

(defn get-game
  [dbspec game-id]
  (let [turns (jdbc/query dbspec
                          [(sql "SELECT g.g_id, g.status g_status,"
                                "t.t_id, t.p_id, t.m_id,"
                                "t.status t_status,"
                                "t.text_turn, t.content"
                                "FROM turn t left join game g"
                                "on t.g_id = g.g_id"
                                "WHERE g.g_id = ?"
                                "ORDER BY t.t_id ASC")
                           game-id])]
    (when (seq turns)
      {:id     game-id
       :status (-> turns first :g_status)
       :turns  (mapv db-turn->turn turns)})))

(defn get-unplayed-game
  "Returns an available game that is untouched by player"
  [dbspec player-id]
  (let [game (some->> (jdbc/query dbspec
                                  [(sql "SELECT g_id FROM game"
                                        "WHERE status = 'available'"
                                        "AND g_id NOT IN"
                                        "(SELECT g_id FROM turn WHERE p_id = ?)"
                                        "LIMIT 1")
                                   player-id])
                      first
                      :g_id
                      (get-game dbspec))]
    (if game
      (log "Found AVAILABLE game" player-id (:id game))
      (log "No AVAILABLE games" player-id))
    game))

(defn set-game-waiting
  [dbspec game-id]
  (log "Setting game WAITING" nil game-id)
  (jdbc/update! dbspec :game {:status "waiting"}
                ["g_id = ? AND status = 'available'"
                 game-id]))

(defn set-game-done
  [dbspec game-id]
  (log "Setting game DONE" nil game-id)
  (jdbc/update! dbspec :game {:status "done"}
                ["g_id = ? AND status = 'available'"
                 game-id]))

(defn set-game-available
  [dbspec game-id]
  (log "Setting game AVAILABLE" nil game-id)
  (jdbc/update! dbspec :game {:status "available"}
                ["g_id = ? AND status = 'waiting'"
                 game-id]))
(defn new-turn
  "Creates new turn for player"
  [dbspec game-id player-id]
  (let [game (get-game dbspec game-id)
        turn (some->> {:p_id      player-id
                       :g_id      game-id
                       :text_turn (-> game
                                      get-last-turn
                                      :text-turn?
                                      not)}
                      (jdbc/insert! dbspec :turn)
                      first
                      db-turn->turn)]
    (log "Created turn" player-id game-id (:id turn))
    (set-game-waiting dbspec game-id)
    turn))

(defn get-turn
  [dbspec player-id]
  (let [turn (some->> [(sql "SELECT t_id, p_id, g_id, m_id,"
                            "status t_status, text_turn, content"
                            "FROM turn WHERE p_id = ?"
                            "AND status <> 'done'")
                       player-id]
                      (jdbc/query dbspec)
                      first
                      db-turn->turn)]
    (if turn
      (log "Got UNPLAYED turn" player-id (:game-id turn) (:id turn))
      (log "No UNPLAYED turn" player-id))
    turn))

(defn get-last-done-turn-in-game
  [dbspec game-id]
  (let [turn (some->> [(sql "SELECT t_id, p_id, g_id, m_id,"
                            "status t_status, text_turn, content"
                            "FROM turn WHERE g_id = ?"
                            "AND status = 'done'"
                            "ORDER by m_at DESC")
                       game-id]
                      (jdbc/query dbspec)
                      first
                      db-turn->turn)]
    (if turn
      (log "Got last DONE turn from game" (:player-id turn) (:id turn) game-id)
      (log "No DONE turns in game" nil game-id nil))
    turn))

(defn play-turn
  [dbspec turn-id message-id content]
  (log/info "Playing turn")
  (jdbc/update! dbspec :turn {:content content
                              :status  "done"
                              :m_id    message-id}
                [(sql "t_id = ? AND status = 'unplayed'")
                 turn-id])
  (let [game-id (:g_id (first (jdbc/query dbspec ["SELECT g_id FROM turn WHERE t_id = ?" turn-id])))]
    (set-game-available dbspec game-id)))




