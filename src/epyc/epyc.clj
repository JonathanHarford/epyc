(ns epyc.epyc
  (:require [clojure.tools.logging :as log]
            [epyc.db :as db]
            [epyc.sender :as send]
            [epyc.text :as txt]))

(defn ^:private first-turn? [turn]
  (-> turn :preceding not))

(defn ^:private text-turn? [turn]
  (:text-turn? turn))

(defn send-turn
  "Send an unplayed turn to the player"
  [{sender :sender} turn]
  (let [player-id (-> turn :player-id)]
    (cond
      (first-turn? turn)
      (send/send-text sender player-id txt/first-turn)

      ;; (text-turn? turn)
      ;; (send/text-turn player-id turn)

      ;; :else
      ;; (send/photo-turn player-id turn)
      )))

(defn join-game
  "Attach a player to a game, creating a new game if necessary"
  [{db     :db
    sender :sender
    :as    ctx} player-id]
  (log/info "join-game")
  (if-let [turn (db/get-turn db player-id)]
    (do
      (send/send-text sender player-id txt/already-playing)
      (send-turn ctx turn))
    (let [game-id (or #_(first (db/get-unplayed-games db player-id))
                      (db/new-game db player-id))
          turn    (db/new-turn db game-id player-id)]
      (send-turn ctx turn))))

(defn play-turn
  "Convert a turn from `unplayed` to `played`"
  [{db                               :db
    sender                           :sender
    {turns-per-game :turns-per-game} :opts} player-id text photo]
  (prn "todo: play-turn")
  #_(let [player (db/get-player db player-id)
          turn   (db/get-turn db player-id)]
      (if turn
        (db/play-turn (:id turn) photo text)
        (send/confused sender player))))

(defn receive-message
  "Respond to a message received from a player"
  ([ctx message-id player text]
   (receive-message ctx message-id player text nil))
  ([{:as    ctx
     sender :sender
     db     :db} message-id player text photo]
   (case text
     "/start"
     (do (db/new-player db player)
         (send/send-text sender (:id player) txt/start))

     "/help"
     (send/send-text sender (:id player) txt/help)

     "/play"
     (join-game ctx (:id player))

     ;; default
     ;; TODO: forward a real turn instead of forwarding back to player
     (send/forward-message sender (:id player) (:id player) message-id)
     #_ (play-turn ctx (:id player) text photo))))
