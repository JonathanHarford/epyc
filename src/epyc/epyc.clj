(ns epyc.epyc
  (:require [epyc.sender :as send]))

(defprotocol IEpyc
  (receive-turn [this turn text photo])
  #_(send-turn [this turn])
  #_(join-game [this player-id])
  #_(play-turn [this player-id photo text]))

(defrecord Epyc
    [sender]
  IEpyc
  (receive-turn [{sender :sender} player-id text photo]
    (send/send-text sender player-id "Turn received") )
  #_(send-turn [{db     :db
               sender :sender} turn]
    (let [player-id (-> turn :player-id)]
      (cond
        (turn/first? turn)
        (send/first-turn player-id)

        (turn/text? turn)
        (send/text-turn player-id turn)

        :else
        (send/photo-turn player-id turn))))
  #_(join-game [{db     :db
               sender :sender} player-id]
    (let [player (player/get db player-id)]
      (if-let [active-turn (turn/get db player-id)]
        (do
          (send/already-playing player active-turn)
          (send-turn active-turn))
        (let [active-turn (turn/new db player-id)]
          (send-turn active-turn)))))
  #_(play-turn [{db     :db
               sender :sender} player-id photo text]
    (let [player (player/get db player-id)
          turn   (turn/get player-id)]
      (if turn
        (turn/play turn player-id photo text)
        (send/confused player)))))





