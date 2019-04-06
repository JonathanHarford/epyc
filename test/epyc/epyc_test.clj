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
    (>!! ch [:send-text player-id text]))
  (send-photo [{ch :ch} player-id photo]
    (>!! ch [:send-photo player-id photo])))

(def player {:id 42})

(def turn {:player-id  42
           :preceding  nil
           :text-turn? true})

(defn create-epyc
  "Create an EPYC with a mocked sender"
  []
  (let [sender (->MockSender (chan (buffer 10)))
        db     (db/->Db "postgresql://localhost:5432/epyctest")]
    (db/migrate-schema db (slurp "resources/migration.sql"))
    (db/drop-data db)
    [(epyc/->Epyc db sender)
     (:ch sender)]))

(deftest receiving-help-command-returns-help-text
  (let [[epyc sender-ch]   (create-epyc)]
    (epyc/receive-message epyc (:id player) "/help" nil)
    (is (= [:send-text (:id player) txt/help]
           (<!! sender-ch)))))

;; (deftest receiving-play-request-creates-player
;;   (let [epyc   (create-epyc)]
;;     (is (= [(:id player) "uh joining game?"]
;;            (epyc/receive-message epyc (:id player) "/play" nil)))
;;     (is (= player
;;            (epyc/get-player))))
;;   )
