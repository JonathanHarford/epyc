(ns epyc.epyc-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.core.async :as async]
   epyc.sender
   [epyc.db :as db]
   [epyc.epyc :refer [receive-message]]
   [epyc.text :as txt]))

(defrecord MockSender
    [ch]
  epyc.sender/ISender
  (send-text [{ch :ch} player-id content]
    (async/>!! ch [player-id content]))
  (forward-message [{ch :ch} player-id from-id message-id]
    (async/>!! ch [player-id from-id message-id])))

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
(def zaphod {:id         666
             :first_name "Zaphod"
             :last_name  "Beeblebrox"})
(def trillian {:id         1000
               :first_name "Trillian"
               :last_name  "Astra"})

(def message-id-counter (atom 0))
(defn ^:private m# []
  @message-id-counter)
(defn ^:private m+ []
  (swap! message-id-counter inc)
  (m#))

(defn ^:private create-epyc
  "Create an EPYC with a test db and a mocked sender. Returns itself, a db, and the channel for the sender."
  []
  (let [sender (->MockSender (async/chan (async/buffer 10)))
        dbspec "postgresql://localhost:5432/epyctest"]
    (db/drop-data dbspec)
    (db/migrate-schema dbspec (slurp "resources/migration.sql"))
    [{:db     dbspec
      :sender sender
      :opts   {:turns-per-game 2}} dbspec (:ch sender)]))

(defn assert-msgs [ch expected-to & expected-msgs]
  (doseq [msg expected-msgs]
    (is (= [(:id expected-to) msg]
           (<!!t ch)))))

(defn assert-fwd [ch expected-to expected-from]
  (let [[actual-to actual-from] (<!!t ch)]
    (is (= [(:id expected-to) (:id expected-from)]
           [actual-to actual-from]))))

(deftest receiving-commands
  (println "-----------------")
  (let [[epyc db ch] (create-epyc)]
    (testing "/start creates player"
      (receive-message epyc (m+) arthur "/start")
      (is (= arthur
             (db/get-player db (:id arthur))))
      (assert-msgs ch arthur txt/start))
    (testing "/help"
      (receive-message epyc (m+) arthur "/help")
      (assert-msgs ch arthur txt/help))
    (testing "Arthur /play"
      (receive-message epyc (m+) arthur "/play")
      (let [expected-turn {:id         1
                           :player-id  (:id arthur)
                           :status     "unplayed"
                           :game-id    1
                           :message-id nil
                           :text-turn? true
                           :content    nil}
            game          (db/get-game db 1)]
        (is (= expected-turn
               (db/get-turn db (:id arthur))))
        (is (= "waiting"
               (:status game)))
        (is (= 1
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last)))
        (assert-msgs ch arthur txt/first-turn)))

    ;; 1 a
    (testing "Arthur /play when epyc is waiting for turn"
      (receive-message epyc (m+) arthur "/play")
      (let [expected-turn {:id         1
                           :player-id  (:id arthur)
                           :status     "unplayed"
                           :game-id    1
                           :message-id nil
                           :text-turn? true
                           :content    nil}
            game          (db/get-game db 1)]
        (is (= expected-turn
               (db/get-turn db (:id arthur))))
        (is (= "waiting"
               (:status game)))
        (is (= 1
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last)))
        (assert-msgs ch arthur
                     txt/already-playing
                     txt/first-turn)))
    ;; 1 a
    (testing "Ford /play with no open games creates game"
      (receive-message epyc (m+) ford "/start")
      (receive-message epyc (m+) ford "/play")
      (let [expected-turn {:id         2
                           :player-id  (:id ford)
                           :status     "unplayed"
                           :game-id    2
                           :message-id nil
                           :text-turn? true
                           :content    nil}
            game          (db/get-game db 2)]
        (is (= expected-turn
               (db/get-turn db (:id ford))))
        (is (= "waiting"
               (:status game)))
        (is (= 1
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last)))
        (assert-msgs ch ford
                     txt/start
                     txt/first-turn)))
    ;; 1 a
    ;; 2 f
    (testing "Arthur completing first (text) turn, game 1"
      (receive-message epyc (m+) arthur "g1t1")
      (let [expected-turn {:id         1
                           :player-id  (:id arthur)
                           :status     "done"
                           :game-id    1
                           :message-id (m#)
                           :text-turn? true
                           :content    "g1t1"}
            game          (db/get-game db 1)]
        (is (= "available"
               (:status game)))
        (is (= 1
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last)))
        (assert-msgs ch arthur txt/turn-done)))
    ;; 1 A
    ;; 2 f
    (testing "Ford completing first (text) turn, game 2"
      (receive-message epyc (m+) ford "g2t1")
      (let [expected-turn {:id         2
                           :player-id  (:id ford)
                           :status     "done"
                           :game-id    2
                           :message-id (m#)
                           :text-turn? true
                           :content    "g2t1"}
            game          (db/get-game db 2)]
        (is (= "available"
               (:status game)))
        (is (= 1
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last)))
        (assert-msgs ch ford txt/turn-done)))
    ;; 1 A
    ;; 2 F
    (testing "Ford /play"
      (receive-message epyc (m+) ford "/play")
      (testing "creates turn 2 on game 1"
        (let [expected-turn {:id         3
                             :player-id  (:id ford)
                             :status     "unplayed"
                             :game-id    1
                             :message-id nil
                             :text-turn? false
                             :content    nil}
              game          (db/get-game db 1)]
          (is (= "waiting"
                 (:status game)))
          (is (= 2
                 (-> game :turns count)))
          (is (= expected-turn
                 (-> game :turns last)))))
      (testing "forwards turn 1 on game 1 and requests photo"
        (assert-msgs ch ford txt/request-photo)
        (assert-fwd ch ford arthur)))
    ;; 1 A f
    ;; 2 F
    (testing "Zaphod /play"
      (receive-message epyc (m+) zaphod "/start")
      (assert-msgs ch zaphod txt/start)
      (receive-message epyc (m+) zaphod "/play")
      (testing "creates turn 2 on game 2"
        (let [expected-turn {:id         4
                             :player-id  (:id zaphod)
                             :status     "unplayed"
                             :game-id    2
                             :message-id nil
                             :text-turn? false
                             :content    nil}
              game          (db/get-game db 2)]
          (is (= "waiting"
                 (:status game)))
          (is (= 2
                 (-> game :turns count)))
          (is (= expected-turn
                 (-> game :turns last)))))
      (testing "forwards turn 1 on game 2 and requests photo"
        (assert-msgs ch zaphod txt/request-photo)
        (assert-fwd ch zaphod ford)))
    ;; 1 A f
    ;; 2 F z
    (testing "Ford tries completing second (pic) turn, game 1 with text"
      (receive-message epyc (m+) ford "g2t1")
      (let [expected-turn {:id         3
                           :player-id  (:id ford)
                           :status     "unplayed"
                           :game-id    1
                           :message-id nil
                           :text-turn? false
                           :content    nil}
            game          (db/get-game db 1)]
        (is (= "waiting"
               (:status game)))
        (is (= 2
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last)))
        (assert-msgs ch ford txt/already-playing txt/request-photo)
        (assert-fwd ch ford arthur))
      ;; 1 A f
      ;; 2 F z
      (testing "Ford completes second turn game 1 with pic"
        (receive-message epyc (m+) ford nil "g2t2")
        (let [expected-turn {:id         3
                             :player-id  (:id ford)
                             :status     "done"
                             :game-id    1
                             :message-id (m#)
                             :text-turn? false
                             :content    "g2t2"}
              game          (db/get-game db 1)]
          (is (= "done"
                 (:status game)))
          (is (= 2
                 (-> game :turns count)))
          (is (= expected-turn
                 (-> game :turns last)))
          (assert-msgs ch ford txt/turn-done))))
    ;; 1 A F
    ;; 2 F z
    ))


