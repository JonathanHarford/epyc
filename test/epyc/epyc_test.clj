(ns epyc.epyc-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.core.async :refer [buffer chan >!! <!!]]
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

(defn create-epyc
  "Create an EPYC with a test db and a mocked sender. Returns itself, a db, and the channel for the sender."
  []
  (let [sender (->MockSender (chan (buffer 10)))
        db     (db/->Db "postgresql://localhost:5432/epyctest")]
    (db/migrate-schema db (slurp "resources/migration.sql"))
    (db/drop-data db)
    [(epyc/->Epyc db sender) db (:ch sender)]))

(deftest receiving-commands
  (let [[epyc db sender-ch]   (create-epyc)]
    (testing "/start creates player"
      (epyc/receive-message epyc arthur "/start" nil)
      (is (= arthur
             (db/get-player db (:id arthur))))
      (is (= [(:id arthur) txt/start]
             (<!! sender-ch))))
    (testing "/help"
      (epyc/receive-message epyc arthur "/help" nil)
      (is (= [(:id arthur) txt/help]
             (<!! sender-ch))))))

#_(deftest receiving-play-request-creates-player
  (let [[epyc sender-ch]   (create-epyc)]
    (epyc/receive-message epyc (:id arthur) "/play" nil)
    (is (= [(:id player) "uh joining game?"]
           (epyc/receive-message epyc (:id arthur) "/play" nil)))
    (is (= arthur
           (epyc/get-player))))
  )
