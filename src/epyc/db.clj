(ns epyc.db
  (:require [epyc.util :refer [spy now]]
            [clojure.java.jdbc :as jdbc]))

(defn ^:private nth-letter [i]
  (str (char (+ i 65))))

(defprotocol State
  (migrate-schema [this schema])
  (drop-data [this])
  (create-player [this handle email])
  (get-player [this email])
  (create-game [this title current-turn current-player])
  (load-data [this data]))

(defn ->current-player [starting-player current-turn])

(defrecord DbState [spec round-prefix]
  State
  (migrate-schema [{spec :spec} schema]
    (when-not (-> (jdbc/query spec
                              [(str "select count(*) from information_schema.tables "
                                    "where table_name in ('game', 'player')")])
                  first :count pos?)
      (print "Creating database structure...") (flush)
      (jdbc/execute! spec [schema])
      (println " done")))
  (drop-data [{spec :spec}]
    (jdbc/execute! spec ["TRUNCATE TABLE player CASCADE; TRUNCATE TABLE game CASCADE"]))
  (create-player [{spec :spec} handle email]
    (println "Creating" handle)
    (jdbc/insert! spec :player {:email email :handle handle}))
  (get-player [{spec :spec} email]
    (jdbc/query spec ["SELECT * FROM player WHERE email = ?"] email))
  (create-game [{spec :spec} title current-turn starting-player]
    (println "Creating" title starting-player "at turn" current-turn)
    (jdbc/insert! spec :game {:title          title
                              :current_turn   current-turn
                              :starting_player starting-player}))
  (load-data [{:keys [round-prefix] :as this} {:keys [player-rows game-rows]}]
    (let [game-rows-with-titles (map-indexed
                                 (fn [idx game-row]
                                   (let [title (str round-prefix (nth-letter idx))]
                                     (conj game-row title)))
                                 game-rows)]
      (doseq [[handle email] player-rows]
        (create-player this handle email))
      (doseq [[player-ix current-turn title] game-rows-with-titles]
        (create-game this title current-turn (get-in player-rows [player-ix 1]))))))
