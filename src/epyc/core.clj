(ns epyc.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [epyc.text :as txt]
            [morse.handlers :as h]
            [morse.api :as t]
            [morse.polling :as p]
            [environ.core :refer [env]]))

(def telegram-token (env :telegram-token))

(defn send-text [id text]
  (t/send-text telegram-token id {:parse_mode "Markdown"} text))

(defn send-photo [id filename]
  (t/send-photo telegram-token id (io/file (io/resource filename))))

(defn start [msg]
  (log/info "/start" msg)
  (send-text (-> msg :from :id) txt/start))

(defn help [msg]
  (log/info "/help" msg)
  (send-text (-> msg :from :id) txt/help))

(defn play [msg]
  (log/info "/play" msg)
  (send-text (-> msg :from :id) "Let's play"))

(defn message-fn [msg]
  (log/info "Intercepted message:" msg)
  (send-text (-> msg :from :id) "Yeah, I hear you."))

(defn -main []
  (log/info "Starting")
  (let [
        ;; sender (send/->Sender telegram-token)
        ;; db     (db/->Db db-spec)
        ;; epyc   (epyc/->Epyc db sender)
        bot-api (h/handlers
                 (h/command-fn "start" start)
                 (h/command-fn "help" help)
                 (h/command-fn "play" play)
                 ;; Handlers will be applied until there are any of those
                 ;; returns non-nil result processing update.
                 ;; Note that sending stuff to the user returns non-nil
                 ;; response from Telegram API.
                 ;; So match-all catch-through case would look something like this:
                 (h/message-fn message-fn))]
    (def channel (p/start telegram-token bot-api {:timeout 65536}))

    ;; TODO: make the Telegram thread non-background instead
    (doall
     (repeatedly 10000000
                 (fn []
                   (log/info "Sleeping")
                   (Thread/sleep 60000))))

    (log/info "Bye")))

