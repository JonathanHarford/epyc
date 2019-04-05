(ns epyc.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [epyc.util :refer [spy]]
            [epyc.email :as email]
            [epyc.db :as db]
            [environ.core :refer [env]]))

(def start-data-filepath "resources/start-data.edn")

(def players ["player1@email.com"
                                        ;...
              ])

(defn next-player [player]
  (let [nexts   (concat (rest players) (take 1 players))
        next-map (zipmap players nexts)]
    (get next-map player)))

(defn -main
  [& args]
  (let [db                (db/->DbState (env :db-spec) (env :round-prefix))
        messenger         (email/->EmailMessenger (env :imap-host)
                                                  (env :smtp-host)
                                                  (env :email-address)
                                                  (env :email-password)
                                                  (env :email-folder))]
    #_(db/migrate-schema db (slurp "resources/migration.sql"))
    #_(when-let [start-data (and (.exists (io/file start-data-filepath))
                               (edn/read-string (slurp start-data-filepath)))]
      (println "Rebuilding data...")
      (db/drop-data db)
      (db/load-data db start-data))
    (let [messages (take 1 (email/get-messages messenger))]
      (doseq [message messages]
        (prn message)))))


