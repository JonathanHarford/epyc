(ns epyc.epyc
  (:require [epyc.db :as db]
            [epyc.sender :as send]
            [epyc.text :as txt]))

(defprotocol IEpyc
  (receive-message [this turn text photo])
  (send-turn [this turn])
  (join-game [this player-id])
  (play-turn [this player-id text photo]))

(defn ^:private first-turn? [turn]
  (-> turn :preceding not))

(defn ^:private text-turn? [turn]
  (:text-turn? turn))

(defrecord Epyc
    [db sender]
  IEpyc

  (receive-message [{:as    this
                     sender :sender
                     db     :db} player text photo]
    (case text
      "/start"
      (do
        (db/new-player db player)
        (send/send-text sender (:id player) txt/start))
      "/help"
      (send/send-text sender (:id player) txt/help)
      "/play"
      (join-game this (:id player))
      ;; default
      (play-turn this (:id player) text photo)))

  (send-turn [{db     :db
               sender :sender} turn]
    (prn "send-turn")
    #_(let [player-id (-> turn :player-id)]
      (cond
        (first-turn? turn)
        (send/first-turn player-id)

        (text? turn)
        (send/text-turn player-id turn)

        :else
        (send/photo-turn player-id turn))))

  (join-game [{db     :db
               sender :sender} player-id]
    (prn "join-game")
    #_(let [player (db/get-player db player-id)]
        (if-let [active-turn (db/get-turn db player-id)]
        (do
          (send/send-text sender player-id txt/already-playing)
          (send-turn active-turn))
        (let [active-turn (db/new-turn db player-id)]
          (send-turn active-turn)))))
  (play-turn [{db     :db
               sender :sender} player-id text photo]
    (prn "todo")
    #_(let [player (player/get db player-id)
            turn     (turn/get player-id)]
        (if turn
          (turn/play turn player-id photo text)
          (send/confused player)))))





