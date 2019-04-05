(ns epyc.state
  (:require [epyc.util :refer [spy now]]))

#_(defprotocol State
  (create-state [overrides])
  (get-player [id])
  (lookup-player [criteria])
  (create-player [player])
  (delete-player [id])
  (create-game [game])
  (get-game [id])
  (create-turn [game player])
  (play-turn [game player content])
  (get-turn [db player])

  )
(defn create-state [overrides]
  (atom (merge {:games   {}
                :players {}}
               overrides)))

(defn ^:private assoc-db [db keys val]
  (swap! db assoc-in keys val))

(defn ^:private update-db [db keys update-fn & update-args]
  (apply swap! db update-in keys update-fn update-args))

(defn ^:private ->new-id [db keypath]
  {:post [(nat-int? %)]}
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
    (println (str "Creating player " id " (" (:email player) ")"))
    (let [db-player (assoc player
                   :id id
                   :c-at (now)
                   :u-at (now))]
      (assoc-db db [:players id] db-player)
      db-player)))

(defn delete-player [db {:keys [id]}]
  (update-db db [:players] dissoc id))

(defn create-game [db]
  (let [id (->new-id db [:games])
        game {:id    id
              :c-at  (now)
              :u-at (now)
              :turns []}]
    (println "Creating game" id)
    (assoc-db db [:games id] game)
    game))

(defn get-game [db game-id]
  (get-in @db [:games game-id]))

(defn get-next-turn-id [game]
  {:post [(nat-int? %)]}
  (print (str "Getting turn " (:id game) "."))
  (spy (-> game :turns count)))

(defn peek-last-turn [game]
  (last (get game :turns)))

(defn pop-last-turn [game]
  (pop (get game :turns)))

(defn text-turn? [game]
  (or (-> game peek-last-turn :text-turn? not)
      true))

(defn create-turn
  ([db game player]
   (println (str "Creating turn " (:id game) "." (get-next-turn-id game)))
   (update-db db [:games (:id game) :turns] conj {:player     (:id player)
                                                  :text-turn? (text-turn? game)

                                                  :played?    false
                                                  :c-at       (now)
                                                  :u-at       (now)})))

(defn update-turn [db game player content]
  (let [turn (spy (merge (spy (pop-last-turn game)) {:content content
                                                     :played? true
                                                     :u-at    (now)}))]
    (assert (= (:player turn) (:id player)))
    (assert (not (:played? turn)))
    (update-db db [:games (:id game) :turns] conj turn)))

(defn played? [{:keys [turns] :as game}
               {:keys [id] :as player}]
  ((->> turns
        (map :player)
        set) id))

(defn waiting? [game {player-id :id}]
  (let [turn (peek-last-turn game)]
    (and (= player-id (:player turn))
         (not (:played? turn)))))

(defn get-waiting-game [db player]
  (some #(waiting? % player) (:games db)))

(defn get-unplayed-game [db player]
  (some #(not (played? % player)) (:games db)))



