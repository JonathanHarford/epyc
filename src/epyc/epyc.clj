(ns epyc.epyc
  (:require [epyc.sender :as send]))

#_(defprotocol IEpyc
  (send-turn [this turn])
  (join-game [this player-id])
  (play-turn [this player-id photo text]))

#_(defrecord Epyc
    [db]
  IEpyc
  (send-turn [{db     :db
               sender :sender} turn]
    (let [player-id (-> turn :player-id)]
      (cond
        (turn/first? turn)
        (send/first-turn player-id)

        (turn/text? turn)
        (send/text-turn player-id turn)

        :else
        (send/photo-turn player-id turn))))
  (join-game [{db     :db
               sender :sender} player-id]
    (let [player (player/get db player-id)]
      (if-let [active-turn (turn/get db player-id)]
        (do
          (send/already-playing player active-turn)
          (send-turn active-turn))
        (let [active-turn (turn/new db player-id)]
          (send-turn active-turn)))))
  (play-turn [{db     :db
               sender :sender} player-id photo text]
    (let [player (player/get db player-id)
          turn   (turn/get player-id)]
      (if turn
        (turn/play turn player-id photo text)
        (send/confused player)))))





