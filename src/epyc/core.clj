(ns epyc.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [epyc.epyc :as epyc]
            [epyc.db :as db]
            [epyc.sender :as send]
            [morse.handlers :as h]
            [morse.polling :as p]
            [environ.core :refer [env]]))

(def telegram-token (env :telegramtoken))
(def dbspec (env :dbspec))
(def turns-per-game (Integer/parseInt (env :turnspergame)))

(defn message-fn [epyc {:keys [message_id from text photo animation video] :as msg}]
  (let [player (select-keys from [:id :first_name :last_name])]
    (log/info (:id player) text)
    (epyc/receive-message epyc
                          message_id
                          player
                          (not-empty text)
                          (or photo animation video))))

(defn -main []
  (log/info "Starting")
  (jdbc/with-db-connection [db-con dbspec]
    (let [sender  (send/->Sender telegram-token)
          epyc    {:db     db-con
                   :sender sender
                   :opts   {:turns-per-game turns-per-game}}
          handler (h/message-fn (partial message-fn epyc))
          channel (p/start telegram-token handler {:timeout 65536})]
      (db/migrate-schema db-con (slurp "resources/migration.sql"))
      (log/info "Open for business.")
      (doall
       (repeatedly 10000000
                   (fn []
                     (print ".")
                     (Thread/sleep 60000))))

      (log/info "Bye")
      (p/stop channel))))

