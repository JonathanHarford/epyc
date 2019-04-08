(ns epyc.epyc-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.core.async :refer [buffer chan >!! <!!]]
   [clojure.tools.logging :as log]
   epyc.sender
   [epyc.db :as db]
   [epyc.epyc :as epyc]
   [epyc.text :as txt]))

(defrecord MockSender
    [ch]
  epyc.sender/ISender
  (send-text [{ch :ch} player-id text]
    (>!! ch [player-id text]))
  (send-photo [{ch :ch} player-id photo]
    (>!! ch [player-id photo])))

(def arthur {:id         13
           :first_name "Arthur"
           :last_name  "Dent"})
(def ford {:id         42
           :first_name "Ford"
           :last_name  "Prefect"})
(def zaphod {:id         1000
             :first_name "Zaphod"
             :last_name  "Beeblebrox"})

(defn ^:private create-epyc
  "Create an EPYC with a test db and a mocked sender. Returns itself, a db, and the channel for the sender."
  []
  (let [sender (->MockSender (chan (buffer 10)))
        db     (db/->Db "postgresql://localhost:5432/epyctest")]
    (db/drop-data db)
    (db/migrate-schema db (slurp "resources/migration.sql"))
    [(epyc/->Epyc db sender) db (:ch sender)]))

(deftest receiving-commands
  (log/info "-----------------")
  (let [[epyc db sender-ch] (create-epyc)]
    (testing "/start creates player"
      (epyc/receive-message epyc arthur "/start" nil)
      (is (= arthur
             (db/get-player db (:id arthur))))
      (is (= [(:id arthur) txt/start]
             (<!! sender-ch))))
    (testing "/help"
      (epyc/receive-message epyc arthur "/help" nil)
      (is (= [(:id arthur) txt/help]
             (<!! sender-ch))))
    (testing "/play"
      (epyc/receive-message epyc arthur "/play" nil)
      (let [expected-turn {:id         1
                           :player-id  (:id arthur)
                           :status     "unplayed"
                           :game-id    1
                           :text-turn? true}]
        (is (= expected-turn
               (db/get-turn db (:id arthur))))
        (is (= {:id  1
                :status "active"
                :turns [expected-turn]}
               (db/get-game db 1)))
        (is (= [(:id arthur) txt/first-turn]
               (<!! sender-ch)))))))


