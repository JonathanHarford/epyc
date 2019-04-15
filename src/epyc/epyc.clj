(ns epyc.epyc
  (:require [taoensso.timbre :as log]
            [epyc.db :as db]
            [epyc.sender :as send]
            [epyc.text :as txt]))

(defn ^:private text-turn? [turn]
  (:text-turn? turn))

(defn send-turn
  "Send an unplayed turn to the player"
  [{sender :sender
    db     :db} turn]
  (let [player-id (-> turn :player-id)
        prev-turn (db/get-last-done-turn-in-game db (:game-id turn))]
    (cond
      (nil? prev-turn)
      (send/send-text sender player-id txt/first-turn)

      (text-turn? turn)
      (do
        (send/send-text sender player-id txt/request-text)
        (send/forward-message sender
                              player-id
                              (:player-id prev-turn)
                              (:message-id prev-turn)))

      :else-photo-turn
      (do
        (send/send-text sender player-id txt/request-photo)
        (send/forward-message sender
                              player-id
                              (:player-id prev-turn)
                              (:message-id prev-turn))))))

(defn resend-turn [{sender :sender
                    :as    ctx} turn]
  (log/info (format "P%s [%s/%s] already playing"
            (:player-id turn)
            (:game-id turn)
            (:id turn)))
  (send/send-text sender (:player-id turn) txt/already-playing)
  (send-turn ctx turn))

(defn join-game
  "Attach a player to a game, creating a new game if necessary"
  [{db     :db
    :as    ctx} player-id]
  (let [existing-turn (db/get-turn db player-id)
        unplayed-game (db/get-unplayed-game db player-id)]
    (cond
      existing-turn
      (resend-turn ctx existing-turn)

      unplayed-game
      (send-turn ctx (db/new-turn db (:id unplayed-game) player-id))

      :else-create-new-game
      (let [new-game-id (db/new-game db player-id)]
        (send-turn ctx (db/new-turn db new-game-id player-id))))))

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
   (log/info (str (:first_name player) " says:") text)
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
     (play-turn ctx (:id player) text photo))))
