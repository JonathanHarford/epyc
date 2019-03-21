(ns epyc-email.game-test
  (:require [clojure.test :refer [deftest is testing]]
            [epyc-email.state :as state]
            [epyc-email.game :as g]))

(def p0 {:id 0 :email "0@e"})
(def p1 {:id 1 :email "1@e"})
;; (def p2 {:id 2 :email "2@e"})
;; (def p3 {:id 3 :email "3@e"})
;; (def p4 {:id 4 :email "4@e"})

(deftest a-test
  (println "STARTING TEST")
  (let [db (state/create-state {:opts {:turns-per-game 2}})]
    (g/request-turn db (:email p0))
    (is (= p0
           (state/get-player-without-times db 0)))
    (is (empty? (:turns (state/get-game-without-times db 0))))
    (g/request-turn db (:email p1))
    (is (= p1
           (state/get-player db 1)))
    (is (= {:player     (:id p1)
            :text-turn? true
            :played?    false}
           (state/get-turn-without-times db 1 0)))

    (g/take-turn db p0 "t")
    (is (= {:player     (:id p0)
            :text-turn? true
            :played?    true
            :content    "t"}
           (state/get-turn-without-times db 0 0)))

    (g/request-turn db (:email p0))
    (is (= {:player     (:id p0)
            :text-turn? false
            :played?    false}
           (state/get-turn-without-times db 1 1)))

    (g/take-turn db (:email p0) {:game 1
                                 :turn          1
                                 :content       "p"})
    (is (= {:player     (:id p0)
            :text-turn? false
            :played?    true
            :content    "p"}
           (state/get-turn-without-times db 1 1)))
    (is (= true
           (:complete? (state/get-game-without-times 1))))

    (g/request-turn db p0)
    (is (= {:player     (:id p0)
            :text-turn? true
            :played?    false}
           (state/get-turn-without-times db 2 0)))
    ))

