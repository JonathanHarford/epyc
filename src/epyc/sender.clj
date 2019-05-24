(ns epyc.sender
  (:require
   [morse.api :as t]))

(defprotocol ISender
  (send-text [this to-id text])
  (forward-message [this to-id from-id message-id]))

(defrecord Sender
    [token]
  ISender
  (send-text [{token :token} to-id text]
    (t/send-text token to-id {:parse_mode "Markdown"} text))

  (forward-message [{token :token} to-id from-id message-id]
    (t/forward-message token to-id from-id message-id)))

