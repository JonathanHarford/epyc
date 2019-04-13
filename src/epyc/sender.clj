(ns epyc.sender
  (:require
   [clojure.java.io :as io]
   [morse.api :as t]))

(defprotocol ISender
  (send-text [this id text])
  (send-photo [this id filename])
  (forward-message [this to-id from-id message-id]))

(defrecord Sender
    [token]
  ISender
  (send-text [{token :token} id text]
    (t/send-text token id {:parse_mode "Markdown"} text))

  (send-photo [{token :token} id filename]
    (t/send-photo token id (io/file (io/resource filename))))

  (forward-message [{token :token} to-id from-id message-id]
    ;; There is a PR on morse to enable this
    #_(t/forward-message token to-id from-id message-id)))

;; (defn text-turn [sender id turn]
;;   (do (send-text sender id txt/request-text)
;;       (send-photo sender
;;                   id
;;                   (io/file (io/resource (-> turn :preceding :filename))))))

;; (defn photo-turn [sender id turn]
;;   (send-text sender
;;              id
;;              (str txt/request-photo "`" (-> turn :preceding :text) "`")))



;; (defn confused [sender id]
;;   (send-text sender id txt/confused))

;; (defn already-playing [sender id]
;;   (send-text id txt/already-playing))

