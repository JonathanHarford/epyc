(ns epyc.epyc
  (:require [taoensso.timbre :as log]
            [epyc.db :as db]
            [epyc.sender :as send]
            [epyc.text :as txt]))

(defn ^:private text-turn? [turn]
  (:text-turn? turn))

(defn ^:private forward-turn
  [{sender :sender} player-id turn]
  (send/forward-message sender
                        player-id
                        (:player-id turn)
                        (:message-id turn)))

(defn ^:private get-player
  [db telegram-user]
  (or (db/get-player db (:id telegram-user))
      (db/new-player db telegram-user)))

(defn ^:private send-turn
  "Send an unplayed turn to the player"
  [{:as    ctx
    sender :sender
    db     :db} turn]
  (let [player-id (-> turn :player-id)
        prev-turn (db/get-last-done-turn-in-game db (:game-id turn))]
    (cond
      (nil? prev-turn)
      (send/send-text sender player-id txt/first-turn)

      (text-turn? turn)
      (do
        (send/send-text sender player-id txt/request-text)
        (forward-turn ctx player-id prev-turn))

      :else-photo-turn
      (do
        (send/send-text sender player-id txt/request-photo)
        (forward-turn ctx player-id prev-turn)))))

(defn ^:private resend-turn [{sender :sender
                              :as    ctx} turn]
  (log/info (format "P%s [%s/%s] already playing"
                    (:player-id turn)
                    (:game-id turn)
                    (:id turn)))
  (send/send-text sender (:player-id turn) txt/already-playing)
  (send-turn ctx turn))

(defn ^:private join-game
  "Attach a player to a game, creating a new game if necessary"
  [{db   :db
    opts :opts
    :as  ctx} player-id]
  (let [existing-turn (db/get-turn db player-id)
        unplayed-game (db/get-unplayed-game db player-id)]
    (cond
      existing-turn
      (resend-turn ctx existing-turn)

      unplayed-game
      (send-turn ctx (db/new-turn db (:id unplayed-game) player-id))

      :else-create-new-game
      (let [new-game-id (db/new-game db player-id (:turns-per-game opts))]
        (send-turn ctx (db/new-turn db new-game-id player-id))))))

(defn ^:private done-turns-count [game]
  (->> game
       :turns
       (filter #(= "done" (:status %)))
       count))

(defn ^:private send-done-game
  [{:as    ctx
    sender :sender} {:keys [turns]}]
  (log/info "Sending done game")
  (doseq [player-id (map :player-id turns)]
    (send/send-text sender player-id txt/game-done-1)
    (doseq [turn turns]
      (log/info (format "Forwarding G%sT%s to P%s"
                        (:game-id turn)
                        (:id turn)
                        player-id))
      (forward-turn ctx player-id turn))
    (send/send-text sender player-id txt/game-done-2)))

(defn ^:private receive-turn
  [{:as    ctx
    db     :db
    sender :sender} player-id message-id text photo]
  (let [turn (db/get-turn db player-id)]
    (cond
      (nil? (:id turn))
      (send/send-text sender player-id txt/confused)

      ;; Expected text, user sent none
      (and (:text-turn? turn) (empty? text))
      (do (log/info (format "P%d should have sent text, but sent nothing" player-id))
          (resend-turn ctx turn))

      ;; Expected text, user sent photo
      (and (:text-turn? turn) (seq photo))
      (do (log/info (format "P%d should have sent text, but sent photo" player-id))
          (resend-turn ctx turn))

      ;; Expected photo, user sent text
      (and (-> turn :text-turn? not) (seq text))
      (do (log/info (format "P%d should have sent photo, but sent %s" player-id text))
          (resend-turn ctx turn))

      ;; Expected photo, user sent none
      (and (-> turn :text-turn? not) (empty? photo))
      (do (log/info (format "P%d should have sent photo, but sent nothing" player-id))
          (resend-turn ctx turn))

      :else
      (do (db/play-turn db (:id turn) message-id (or text photo))
          (send/send-text sender player-id txt/turn-done)))

    (let [game (db/get-game db (:game-id turn))]
      (when (= (:num-turns game) (done-turns-count game))
        (db/set-game-done db (:id game))
        (send-done-game ctx game)))))

(defn receive-message
  "Receive a message from a player in order to respond"
  ([ctx message-id player text]
   (receive-message ctx message-id player text nil))
  ([{:as    ctx
     sender :sender
     db     :db
     opts   :opts} message-id telegram-user text photo]
   (let [player (get-player db telegram-user)]
     (log/info (format "%d-%s says: %s"
                       (:id player)
                       (:first_name player)
                       (if text
                         (str "TEXT:" text)
                         (str "PHOTO:" photo))))
     (case text

       "/start"
       (send/send-text sender (:id player) txt/start)

       "/help"
       (send/send-text sender (:id player) (txt/->help (:turns-per-game opts)))

       "/play"
       (join-game ctx (:id player))

       "/drop" ;; TODO remove when done testing
       (db/drop-data db)

       ;; default
       (receive-turn ctx (:id player) message-id text photo)))))
