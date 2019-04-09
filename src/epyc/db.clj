(ns epyc.db
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [epyc.util :refer [spy]]))

(defn ^:private db-turn->turn
  [{:keys [t_id p_id g_id m_id t_status text_turn text]}]
  (merge {:id         t_id
          :player-id  p_id
          :game-id    g_id
          :message-id m_id
          :status     t_status
          :text-turn? text_turn
          :text       text}))

(defn ^:private get-last-turn [{turns :turns}]
  (last turns))

(defn migrate-schema [dbspec schema]
  (if (-> (jdbc/query
           dbspec
           [(str "SELECT COUNT(*) "
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
  (log/info "Creating" id first_name last_name)
  (jdbc/insert! dbspec
                :player
                {:p_id       id
                 :first_name first_name
                 :last_name  last_name}))


(defn get-player
  [dbspec player-id]
  (log/info "Getting player" player-id)
  (->> [(str "SELECT p_id as id, first_name, last_name "
             "FROM player WHERE p_id = ?") player-id]
       (jdbc/query dbspec)
       first))

(defn new-game
  [dbspec player-id]
  (log/info "Creating game for" player-id)
  (->> {:status "active"}
       (jdbc/insert! dbspec :game)
       first
       :g_id))

(defn get-game
  [dbspec game-id]
  (log/info "Getting game" game-id)
  (let [turns (jdbc/query dbspec
                          [(str "SELECT g.g_id, g.status g_status, t.t_id, "
                                "t.p_id, t.m_id, t.status t_status, t.text_turn "
                                "FROM turn t left join game g "
                                "on t.g_id = g.g_id "
                                "WHERE g.g_id = ?")
                           game-id])]
    {:id     game-id
     :status (-> turns first :g_status)
     :turns  (mapv db-turn->turn turns)}))

(defn new-turn
  "Creates new turn in new game for player"
  [dbspec game-id player-id]
  (log/info "Creating turn in game" game-id "for" player-id)
  (let [game (get-game dbspec game-id)]
    (some->> {:p_id      player-id
              :g_id      game-id
              :text_turn (-> game
                             get-last-turn
                             :text-turn?
                             not)}
             (jdbc/insert! dbspec :turn)
             first
             db-turn->turn)))

(defn get-turn
  [dbspec player-id]
  (log/info "Getting turn for" player-id)
  (some->> [(str "SELECT t_id, p_id, g_id, m_id,"
                 "status t_status, text_turn, text "
                 "FROM turn WHERE p_id = ? "
                 "AND status = 'unplayed'")
            player-id]
           (jdbc/query dbspec)
           first
           db-turn->turn))

#_(defn play-turn
    [dbspec turn-id photo text]
    (log/info "Playing turn" turn-id (if photo photo text))
    (jdbc/update! dbspec :turn {:text text}  ; TODO: photo
                  [(str "WHERE t_id = ? AND status = 'unplayed'")
                   turn-id]))




