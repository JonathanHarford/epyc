(ns epyc.epyc
  (:require [epyc.sender :as send]
            [epyc.text :as txt]))

(defprotocol IEpyc
  (receive-message [this turn text photo])
  #_(send-turn [this turn])
  (join-game [this player-id])
  #_(play-turn [this player-id photo text]))

(defrecord Epyc
    [sender]
  IEpyc
  (receive-message [{:as    this
                     sender :sender} player-id text photo]
    (case text
      "/start"
      (send/send-text sender player-id txt/start)
      "/help"
      (send/send-text sender player-id txt/help)
      "/play"
      (join-game this player-id)
                                        ; default
      (send/send-text sender player-id "Turn received")
      )
    )
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
  (join-game [this player-id]
    (send/send-text sender player-id "uh joining game?"))
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





