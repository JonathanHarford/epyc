(ns epyc.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [epyc.sender :as send]
            [morse.handlers :as h]
            [morse.polling :as p]
            [environ.core :refer [env]]))

(def telegram-token (env :telegram-token))

(defn start [sender msg]
  (log/info "/start" msg)
  (send/start sender (-> msg :from :id)))

(defn help [sender msg]
  (log/info "/help" msg)
  (send/help sender (-> msg :from :id)))

(defn play [msg]
  (log/info "/play" msg))

(defn message-fn [msg]
  (log/info "Intercepted message:" msg))

(defn -main []
  (log/info "Starting")
  (let [sender (send/->Sender telegram-token)
        ;; db     (db/->Db db-spec)
        ;; epyc   (epyc/->Epyc db sender)

        bot-api (fn [x] (h/handling x
                                    (h/command-fn "start" (partial start sender))
                                    (h/command-fn "help" (partial help sender))
                                    (h/command-fn "play" play)
                                    (h/message-fn message-fn)))
        channel (p/start telegram-token bot-api {:timeout 65536})]

    (doall
     (repeatedly 10000000
                 (fn []
                   (print ".")
                   (Thread/sleep 60000))))

    (log/info "Bye")
    (p/stop channel)))

