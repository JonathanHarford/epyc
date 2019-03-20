(ns epyc-email.state
  (:require [epyc-email.util :refer [spy now]]))

(defn create-state [overrides]
  (atom (merge {:games   {}
                :players {}}
               overrides)))

(defn ^:private assoc-db [db keys val]
  (swap! db assoc-in keys val))

(defn ^:private update-db [db keys update-fn & update-args]
  (apply swap! db update-in keys update-fn update-args))

(defn ^:private ->new-id [db keypath]
  (->> (get-in @db keypath)
       keys
       (cons -1)
       (apply max)
       inc))

(defn get-player [db id]
  (get-in @db [:players id]))

(defn get-player-by-email [db email]
  (some #(= (:email %) email) (:players @db)))

(defn create-player [db player]
  (let [id (->new-id db [:players])]
    (assoc-db db [:players id] (assoc player
                                      :id id
                                      :c-at (now)))
    id))

(defn delete-player [db {:keys [id]}]
  (update-db db [:players] dissoc id))

(defn create-game [db]
  (let [id (->new-id db [:games])]
    (assoc-db db [:games id] {:id    id
                              :c-at  (now)
                              :turns []})
    id))

(defn read-game [db game-id]
  (get-in @db [:games game-id]))

(defn get-last-turn-id [game]
  (-> game :turns count))

(defn get-last-turn [game]
  (get-in game [:turns (get-last-turn-id game)]))

(defn create-turn
  ([db game player]
   (let [text-turn? (->> game get-last-turn :text-turn? not)]
     (update-db db [:games (:id game) :turns] conj {:player     (:id player)
                                                    :text-turn? text-turn?
                                                    :played?    false
                                                    :c-at       (now)
                                                    :u-at       (now)}))))

(defn update-turn [db game player content]
  (let [turn-id (get-last-turn-id game)
        turn    (get-in @db [:games (:id game) :turns turn-id])]
    (assert (= (:player turn) (:id player)))
    (assert (not (:played? turn)))
    (update-db db [:games (:id game) :turns turn-id] merge {:content content
                                                            :played? true
                                                            :u-at    (now)})))

(defn played? [{:keys [turns] :as game}
               {:keys [id] :as player}]
  ((->> turns
        (map :player)
        set) id))

(defn get-unplayed-game [db player]
  (some #(not (played? % player)) (:games db)))

(defn get-game-without-times
  [db game-num]
  (dissoc
   (get-in @db [:games game-num] dissoc)
   :c-at :u-at))

(defn get-turn-without-times
  [db game-num turn]
  (dissoc
   (get-in @db [:games game-num :turns turn])
   :c-at :u-at))


