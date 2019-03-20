(ns epyc-email.email
  (:require clojure.walk
            clojure.string
            [epyc-email.util :refer [spy]]
            [clojure-mail.core :as mail]
            [clojure-mail.message :refer (read-message)]
            [postal.core :as postal]))

(defprotocol Messenger
  (get-messages [this])
  (send-message [to image body opts]))

(defn ^:private relevant-content-type? [{:keys [content-type]}]
  (some #(clojure.string/starts-with? content-type %) ["IMAGE/PNG"
                                                       "IMAGE/JPEG"
                                                       "TEXT/PLAIN"]))

(defn ^:private email->relevant-content [{:keys [body]}]
  (->> body
       (mapcat #(if (seq? %) (seq %) [%]))
       (filter relevant-content-type?)
       (map #(hash-map (-> %
                           :content-type
                           (clojure.string/replace #";.+" "")
                           (clojure.string/replace "/" "-")
                           (clojure.string/lower-case)
                           keyword)
                       (:body %)))
       (apply merge)))

(defn ^:private email->message [javamail-message]
  (let [msg     (read-message javamail-message)
        headers (-> (apply merge (:headers msg))
                    clojure.walk/keywordize-keys
                    (select-keys [:In-Reply-To]))]
    (-> msg
        (select-keys [
                      ;; :to
                      ;; :cc
                      ;; :date-recieved
                      ;; :bcc
                      ;; :headers
                      ;; :body
                      ;; :sender

                      ;; :id
                      ;; :from
                      :subject
                      ;; :date-sent

                      ;; :multipart?
                      ;; :content-type
                      ])
        (merge headers (email->relevant-content msg)))))

(defrecord EmailMessenger
  [host email password]
  Messenger
  (get-messages [{:keys [host email password]}]
    (->> (mail/store host email password)
         mail/inbox
         (map email->message)))

  (send-message [to image body opts]
    (prn "Sending... (not really)")))





