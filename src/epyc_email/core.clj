(ns epyc-email.core
  (:gen-class)
  (:require [epyc-email.util :refer [spy]]
            [epyc-email.email :as email]
            [epyc-email.game :as game]))

(defn -main
  [& args]
  (let [messenger         (email/->EmailMessenger "imap.gmail.com"
                                                 "kitty.spielford@gmail.com"
                                                 "poodleball")]
    (let [turns (->> (.get-messages messenger)
                     (filter #(clojure.string/includes? (:subject %) "TEST")) ;; TODO remove
                     )]
      (spy turns))))

