(ns epyc.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [epyc.sender :as send]
            [morse.handlers :as h]
            [morse.polling :as p]
            [environ.core :refer [env]]))

(def telegram-token (env :telegram-token))

(defn play [msg]
  (log/info "/play" msg))

(defn message-fn [sender {:keys [from text] :as msg}]
  (let [player-id (:id from)]
    (log/info player-id text)
    (cond (= "/start" text)
          (send/start sender player-id)
          (= "/help" text)
          (send/help sender player-id)
          :default
          (send/send-text sender player-id "Thx"))))

(defn -main []
  (log/info "Starting")
  (let [sender (send/->Sender telegram-token)
        ;; db     (db/->Db db-spec)
        ;; epyc   (epyc/->Epyc db sender)
        channel (p/start telegram-token (h/message-fn (partial message-fn sender)) {:timeout 65536})]

    (doall
     (repeatedly 10000000
                 (fn []
                   (print ".")
                   (Thread/sleep 60000))))

    (log/info "Bye")
    (p/stop channel)))

