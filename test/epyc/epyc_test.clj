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
      :opts   {:turns-per-game 3}} dbspec (:ch sender)]))

(deftest receiving-commands
  (log/info "-----------------")
  (let [[epyc db sender-ch] (create-epyc)]
    (testing "/start creates player"
      (epyc/receive-message epyc (m+) arthur "/start" nil)
      (is (= arthur
             (db/get-player db (:id arthur))))
      (is (= [(:id arthur) txt/start]
             (<!!t sender-ch))))
    (testing "/help"
      (epyc/receive-message epyc (m+) arthur "/help" nil)
      (is (= [(:id arthur) txt/help]
             (<!!t sender-ch))))
    (testing "Arthur /play"
      (epyc/receive-message epyc (m+) arthur "/play" nil)
      (let [expected-turn {:id         1
                           :player-id  (:id arthur)
                           :status     "unplayed"
                           :game-id    1
                           :message-id nil
                           :text-turn? true
                           :text       nil}
            game          (db/get-game db 1)]
        (is (= expected-turn
               (db/get-turn db (:id arthur))))
        (is (= "active"
               (:status game)))
        (is (= 1
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last)))
        (is (= [(:id arthur) txt/first-turn]
               (<!!t sender-ch)))))

    ;; 1 a
    (testing "Arthur /play when epyc is waiting for turn"
      (epyc/receive-message epyc (m+) arthur "/play" nil)
      (let [expected-turn {:id         1
                           :player-id  (:id arthur)
                           :status     "unplayed"
                           :game-id    1
                           :message-id nil
                           :text-turn? true
                           :text       nil}
            game          (db/get-game db 1)]
        (is (= expected-turn
               (db/get-turn db (:id arthur))))
        (is (= "active"
               (:status game)))
        (is (= 1
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last))) 
        (is (= [(:id arthur) txt/already-playing]
               (<!!t sender-ch)))
        (is (= [(:id arthur) txt/first-turn]
               (<!!t sender-ch)))))
    ;; 1 a
    (testing "Ford /play with no open games creates game"
      (epyc/receive-message epyc (m+) ford "/start" nil)
      (is (= [(:id ford) txt/start]
             (<!!t sender-ch)))
      (epyc/receive-message epyc (m+) ford "/play" nil)
      (let [expected-turn {:id         2
                           :player-id  (:id ford)
                           :status     "unplayed"
                           :game-id    2
                           :message-id nil
                           :text-turn? true
                           :text       nil}
            game          (db/get-game db 2)]
        (is (= expected-turn
               (db/get-turn db (:id ford))))
        (is (= "active"
               (:status game)))
        (is (= 1
               (-> game :turns count)))
        (is (= expected-turn
               (-> game :turns last)))
        (is (= [(:id ford) txt/first-turn]
               (<!!t sender-ch)))))
    ;; 1 a
    ;; 2 f

    #_(
       (testing "Arthur completing first (text) turn, game 1"
         (epyc/receive-message epyc (m+) arthur "g1t1" nil)
         (let [expected-turn {:id         1
                              :player-id  (:id arthur)
                              :status     "done"
                              :game-id    1
                              :message-id (m#)
                              :text-turn? true
                              :text       "g1t1"}
               game          (db/get-game db 1)]
           (is (= expected-turn
                  (db/get-turn db (:id arthur))))
           (is (= "active"
                  (:status game)))
           (is (= 1
                  (-> game :turns count)))
           (is (= expected-turn
                  (-> game :turns last)))
           (is (= [(:id arthur) txt/turn-done]
                  (<!!t sender-ch)))))
       ;; 1 A
       ;; 2 f
       (testing "Ford completing first (text) turn, game 2"
         (epyc/receive-message epyc (m+) ford "g2t1" nil)
         (let [expected-turn {:id         2
                              :player-id  (:id ford)
                              :status     "done"
                              :game-id    2
                              :message-id (m#)
                              :text-turn? true
                              :text       "g2t1"}
               game          (db/get-game db 2)]
           (is (= expected-turn
                  (db/get-turn db (:id ford))))
           (is (= "active"
                  (:status game)))
           (is (= 1
                  (-> game :turns count)))
           (is (= expected-turn
                  (-> game :turns last)))
           (is (= [(:id arthur) txt/turn-done]
                  (<!!t sender-ch)))))
       ;; 1 A
       ;; 2 F
       (testing "Ford /play"
         (epyc/receive-message epyc (m+) ford "/play" nil)
         (testing "creates turn 2 on game 1"
           (let [expected-turn {:id         3
                                :player-id  (:id ford)
                                :status     "unplayed"
                                :game-id    1
                                :message-id nil
                                :text-turn? false}
                 game          (db/get-game db 1)]
             (is (= expected-turn
                    (db/get-turn db (:id ford))))
             (is (= "active"
                    (:status game)))
             (is (= 2
                    (-> game :turns count)))
             (is (= expected-turn
                    (-> game :turns last)))))
         (testing "forwards turn 1 on game 1 and requests photo"
           (is (= [(:id ford) (:id arthur) 1]
                  (<!!t sender-ch)))
           (is (= [(:id ford) txt/request-photo]
                  (<!!t sender-ch))))
         )
       ;; 1 A f
       ;; 2 F
       (testing "Zaphod /play"
         (epyc/receive-message epyc (m+) zaphod "/play" nil)
         (testing "creates turn 2 on game 2"
           (let [expected-turn {:id         4
                                :player-id  (:id zaphod)
                                :status     "unplayed"
                                :game-id    2
                                :message-id nil
                                :text-turn? false}
                 game          (db/get-game db 2)]
             (is (= expected-turn
                    (db/get-turn db (:id zaphod))))
             (is (= "active"
                    (:status game)))
             (is (= 2
                    (-> game :turns count)))
             (is (= expected-turn
                    (-> game :turns last)))
             ))
         (testing "forwards turn 1 on game 2 and requests photo"
           (is (= [(:id zaphod) (:id ford) 1]
                  (<!!t sender-ch)))
           (is (= [(:id zaphod) txt/request-photo]
                  (<!!t sender-ch)))))
       ;; 1 A f
       ;; 2 F z
       (testing "Ford tries completing second (pic) turn, game 1 with text"
         (epyc/receive-message epyc (m+) ford "g2t1" nil)
         (let [expected-turn {:id         3
                              :player-id  (:id ford)
                              :status     "unplayed"
                              :game-id    1
                              :message-id (m#)
                              :text-turn? true
                              :text       "g2t1"}
               game          (db/get-game db 2)]
           (is (= expected-turn
                  (db/get-turn db (:id ford))))
           (is (= "active"
                  (:status game)))
           (is (= 2
                  (-> game :turns count)))
           (is (= expected-turn
                  (-> game :turns last)))
           (is (= [(:id ford) txt/confused]
                  (<!!t sender-ch)))
           (is (= [(:id ford) txt/request-photo]
                  (<!!t sender-ch))))
         ;; 1 A f
         ;; 2 F z
         (testing "Ford completes second turn game 2 with pic"
           (epyc/receive-message epyc (m+) ford nil "g2t2")
           (let [expected-turn {:id         2
                                :player-id  (:id ford)
                                :status     "done"
                                :game-id    2
                                :message-id (m#)
                                :text-turn? true
                                :photo      "g2t2"}
                 game          (db/get-game db 2)]
             (is (= "active"
                    (:status game)))
             (is (= 2
                    (-> game :turns count)))
             (is (= expected-turn
                    (-> game :turns last)))
             (is (= [(:id ford) txt/turn-done]
                    (<!!t sender-ch)))))
         ;; 1 A F
         ;; 2 F z
         (testing "Trillian /play"
           (epyc/receive-message epyc (m+) trillian "/play" nil)
           (testing "creates turn 3 on game 1"
             (let [expected-turn {:id         5
                                  :player-id  (:id trillian)
                                  :status     "unplayed"
                                  :game-id    1
                                  :message-id nil
                                  :text-turn? true}
                   game          (db/get-game db 1)]
               (is (= "active"
                      (:status game)))
               (is (= 3
                      (-> game :turns count)))
               (is (= expected-turn
                      (-> game :turns last)))
               ))
           (testing "forwards turn 2 on game 1 and requests text"
             (is (= [(:id trillian) (:id ford) 3]
                    (<!!t sender-ch)))
             (is (= [(:id trillian) txt/request-text]
                    (<!!t sender-ch)))))
         ;; 1 A F t
         ;; 2 F z
         (testing "Trillian completes game 2 turn 3 with pic"
           (epyc/receive-message epyc (m+) trillian "g2t3" nil)
           (let [expected-turn {:id         5
                                :player-id  (:id trillian)
                                :status     "done"
                                :game-id    1
                                :message-id (m#)
                                :text-turn? true
                                :text       "g2t2"}
                 game          (db/get-game db 2)]
             (is (= expected-turn
                    (db/get-turn db (:id trillian))))
             (is (= "done"
                    (:status game)))
             (is (= 3
                    (-> game :turns count)))
             (is (= expected-turn
                    (-> game :turns last)))
             (is (= [(:id trillian) txt/turn-done]
                    (<!!t sender-ch)))))
         ))))


