(ns epyc.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [epyc.epyc :as epyc]
            [epyc.sender :as send]
            [morse.handlers :as h]
            [morse.polling :as p]
            [environ.core :refer [env]]))

(def telegram-token (env :telegram-token))
(def db-spec (env :db-spec))

(defn message-fn [epyc {:keys [message_id from text] :as msg}]
  (let [player (select-keys from [:id :first_name :last_name])]
    (log/info (:id player) text)
    (epyc/receive-message epyc message_id player text)))

(defn -main []
  (log/info "Starting")
  (let [sender  (send/->Sender telegram-token)
        epyc    {:db             db-spec
                 :sender         sender
                 :turns-per-game 3}
        handler (h/message-fn (partial message-fn epyc))
        channel (p/start telegram-token handler {:timeout 65536})]

    (doall
     (repeatedly 10000000
                 (fn []
                   (print ".")
                   (Thread/sleep 60000))))

    (log/info "Bye")
    (p/stop channel)))

