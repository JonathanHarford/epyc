(ns epyc.epyc
  (:require [clojure.tools.logging :as log]
            [epyc.db :as db]
            [epyc.sender :as send]
            [epyc.text :as txt]))

(defprotocol IEpyc
  (receive-message [this turn text photo]
    "Respond to a message received from a player")
  (send-turn [this turn]
    "Send an unplayed turn to the player")
  (join-game [this player-id]
    "Attach a player to a game, creating a new game if necessary")
  (play-turn [this player-id text photo]
    "Convert a turn from `unplayed` to `played`"))

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
      (do (db/new-player db player)
          (send/send-text sender (:id player) txt/start))

      "/help"
      (send/send-text sender (:id player) txt/help)

      "/play"
      (join-game this (:id player))

      ;; default
      (prn "todo: play-turn")
      #_(play-turn this (:id player) text photo)))

  (send-turn [{sender :sender} turn]
    (let [player-id (-> turn :player-id)]
      (cond
        (first-turn? turn)
        (send/send-text sender player-id txt/first-turn)

        ;; (text-turn? turn)
        ;; (send/text-turn player-id turn)

        ;; :else
        ;; (send/photo-turn player-id turn)
        )))

  (join-game [{db     :db
               sender :sender
               :as    this} player-id]
    (log/info "join-game")
    (if-let [turn (db/get-turn db player-id)]
      (prn "todo" turn)
      ;; (do
      ;;   (send/send-text sender player-id txt/already-playing)
      ;;   (send-turn this turn))
      (let [game-id (or #_(first (db/get-unplayed-games db player-id))
                        (db/new-game db player-id))
            turn    (db/new-turn db game-id player-id)]
        (send-turn this turn))))

  (play-turn [{db     :db
               sender :sender} player-id text photo]
    (prn "todo: play-turn")
    #_(let [player (db/get-player db player-id)
          turn (db/get-turn db player-id)]
        (if turn
          (db/play-turn (:id turn) photo text)
          (send/confused sender player)))))





