(ns epyc.epyc-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]
   epyc.sender
   [epyc.db :as db]
   [epyc.epyc :as epyc]
   [epyc.text :as txt]))

(defrecord MockSender
    [ch]
  epyc.sender/ISender
  (send-text [{ch :ch} player-id text]
    (async/>!! ch [player-id text]))
  (send-photo [{ch :ch} player-id photo]
    (async/>!! ch [player-id photo]))
  (forward-message [{ch :ch} player-id message-id]
    (async/>!! ch [player-id message-id])))

(defn <!!t
  "Same as <!!, but waits 0.5s"
  [ch]
  (first (async/alts!! [(async/timeout 500) ch])))

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
  (let [sender (->MockSender (async/chan (async/buffer 10)))
        dbspec "postgresql://localhost:5432/epyctest"]
    (db/drop-data dbspec)
    (db/migrate-schema dbspec (slurp "resources/migration.sql"))
    [{:db     dbspec
      :sender sender} dbspec (:ch sender)]))

(deftest receiving-commands
  (log/info "-----------------")
  (let [[epyc db sender-ch] (create-epyc)]
    (testing "/start creates player"
      (epyc/receive-message epyc arthur "/start" nil)
      (is (= arthur
             (db/get-player db (:id arthur))))
      (is (= [(:id arthur) txt/start]
             (<!!t sender-ch))))
    (testing "/help"
      (epyc/receive-message epyc arthur "/help" nil)
      (is (= [(:id arthur) txt/help]
             (<!!t sender-ch))))
    (testing "/play"
      (epyc/receive-message epyc arthur "/play" nil)
      (let [expected-turn {:id         1
                           :player-id  (:id arthur)
                           :status     "unplayed"
                           :game-id    1
                           :message-id nil
                           :text-turn? true
                           :text       nil}]
        (is (= expected-turn
               (db/get-turn db (:id arthur))))
        (is (= {:id     1
                :status "active"
                :turns  [expected-turn]}
               (db/get-game db 1)))
        (is (= [(:id arthur) txt/first-turn]
               (<!!t sender-ch))))

      (testing "/play when epyc is waiting for turn"
        (epyc/receive-message epyc arthur "/play" nil)
        (let [expected-turn {:id         1
                             :player-id  (:id arthur)
                             :status     "unplayed"
                             :game-id    1
                             :message-id nil
                             :text-turn? true
                             :text       nil}]
          (is (= expected-turn
                 (db/get-turn db (:id arthur))))
          (is (= {:id     1
                  :status "active"
                  :turns  [expected-turn]}
                 (db/get-game db 1)))
          (is (= [(:id arthur) txt/already-playing]
                 (<!!t sender-ch)))
          (is (= [(:id arthur) txt/first-turn]
                 (<!!t sender-ch)))))
      (testing "Second player /play with no open games"
        (epyc/receive-message epyc ford "/start" nil)
        (is (= [(:id ford) txt/start]
               (<!!t sender-ch)))
        (epyc/receive-message epyc ford "/play" nil)
        (let [expected-turn {:id         2
                             :player-id  (:id ford)
                             :status     "unplayed"
                             :game-id    2
                             :message-id nil
                             :text-turn? true
                             :text       nil}]
          (is (= expected-turn
                 (db/get-turn db (:id ford))))
          (is (= {:id     2
                  :status "active"
                  :turns  [expected-turn]}
                 (db/get-game db 2)))
          (is (= [(:id ford) txt/first-turn]
                 (<!!t sender-ch))))))
    #_(testing "Completing a turn"
        (epyc/receive-message epyc arthur "t1t" nil)
        (let [expected-turn {:id         1
                             :player-id  (:id arthur)
                             :status     "done"
                             :game-id    1
                             :text-turn? true
                             :text       "t1t"}]
          (is (= expected-turn
                 (db/get-turn db (:id arthur))))
          (is (= {:id     1
                  :status "active"
                  :turns  [expected-turn]}
                 (db/get-game db 1)))
          (is (= [(:id arthur) txt/turn-done]
                 (<!! sender-ch)))))))


