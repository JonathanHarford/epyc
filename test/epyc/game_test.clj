(ns epyc.game-test
  (:require [clojure.test :refer [deftest is testing]]
            [epyc.state :as state]
            [epyc.game :as g]))

(def p0 {:id 0 :email "0@e"})
(def p1 {:id 1 :email "1@e"})
;; (def p2 {:id 2 :email "2@e"})
;; (def p3 {:id 3 :email "3@e"})
;; (def p4 {:id 4 :email "4@e"})

(defn ch= [& xs]
  "Compare equality ignoring created-at & updated-at"
  (apply = (map #(dissoc % :c-at :u-at) xs)))

(deftest full-game
  (println "STARTING TEST")
  (let [db (state/create-state {:opts {:turns-per-game 2}})]

    (g/request-turn db (:email p0))
    (is (ch= p0
             (state/get-player db (:id p0))))
    (is (ch= {:player     (:id p0),
              :text-turn? true,
              :played?    false,}
             (state/peek-last-turn (state/get-game db 0))))

    (g/take-turn db p0 "text")
    (is (ch= {:player     (:id p0),
              :text-turn? true,
              :played?    true,}
             (state/peek-last-turn (state/get-game db 0))))

    (g/request-turn db (:email p1))
    (is (ch= p1
             (state/get-player db 1)))
    (is (ch= {:player     (:id p1)
              :text-turn? false
              :played?    false}
           (state/peek-last-turn (state/get-game db 0))))

    (g/take-turn db p1 "pic")
    (is (ch= {:player     (:id p1)
              :text-turn? false
              :played?    true
              :content    "pic"}
             (state/peek-last-turn (state/get-game db 0))))

    (let [finished-game (state/get-game db 0)]
      (is (= true
             (:finished? finished-game))))))

