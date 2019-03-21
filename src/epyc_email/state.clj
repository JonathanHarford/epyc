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
    (assoc-db db [:players id] (assoc player
                                      :id id
                                      :c-at (now)))
    id))

(defn delete-player [db {:keys [id]}]
  (update-db db [:players] dissoc id))

(defn create-game [db]
  (let [id (->new-id db [:games])
        game {:id    id
              :c-at  (now)
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
   (println "Creating turn" (:id game) "." (get-next-turn-id game))
   (update-db db [:games (:id game) :turns] conj {:player     (:id player)
                                                  :text-turn? (text-turn? game)

                                                  :played?    false
                                                  :c-at       (now)
                                                  :u-at       (now)})))

(defn update-turn [db game player content]
  (let [turn (merge (pop-last-turn game) {:content content
                                          :played? true
                                          :u-at    (now)})]
    (assert (= (:player turn) (:id player)))
    (assert (not (:played? turn)))
    (update-db db [:games (:id game) :turns] conj turn)))

(defn played? [{:keys [turns] :as game}
               {:keys [id] :as player}]
  ((->> turns
        (map :player)
        set) id))

(defn get-unplayed-game [db player]
  (some #(not (played? % player)) (:games db)))

(defn get-game-without-times
  [db game-id]
  (dissoc (get-game db game-id) :c-at :u-at))

(defn get-player-without-times
  [db player-id]
  (dissoc (get-player db player-id) :c-at :u-at))

(defn get-turn-without-times
  [db game-id turn-id]
  (let [game (get-game db game-id)
        turn (peek-last-turn game)]
    (assert (= (:id turn) turn-id))
    (dissoc turn :c-at :u-at)))


