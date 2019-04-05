(ns epyc.game
  (:require [epyc.util :refer [spy]]
            [epyc.state :as s]))

(defn get-or-create-player [db email]
  (or (s/get-player-by-email db email)
      (s/create-player db {:email email})))

(defn get-or-create-playable-game [db player]
  (or (s/get-waiting-game db player)
      (s/create-game db)))

(defn request-turn
  [db email]
  (let [player (get-or-create-player db email)
        game   (get-or-create-playable-game db player)]
    (s/create-turn db game player)))

(defn take-turn
  [db player content]
  (let [game   (s/get-unplayed-game db player)]
    (s/update-turn db game player content)))