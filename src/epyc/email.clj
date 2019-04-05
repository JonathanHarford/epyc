(ns epyc.email
  (:require clojure.string
            [clojure.java.io :as io]
            [epyc.util :refer [spy uuid]]
            [clojure-mail.core :as mail]
            clojure-mail.message
            [postal.core :as postal]))

(defprotocol Messenger
  (get-messages [this])
  (forward-message [this to message opts]))


(defn filename->mime [filename]
  (str "image/" (second (clojure.string/split filename "."))))

(defn mime->filename [m]
  (let [ext (-> m
                (clojure.string/replace #"^.+/" "")
                clojure.string/lower-case)]
    (str (uuid) "." ext)))

(defn save-attachment [stream mime]
  (println "Saving" mime)
  (let [filename (mime->filename mime)]
    (with-open [i (io/input-stream stream)]
      (io/copy i (io/file filename)))
    filename))

(defn email->relevant-content
  [{:keys [body] :as email}]
  (let [{:keys [body mime]} (->> body
                                 flatten
                                 (map (fn [att]
                                        (assoc att :mime (-> att
                                                             :content-type
                                                             (clojure.string/replace #";.+" "")
                                                             clojure.string/lower-case))))
                                 (sort-by (fn [{:keys [mime]}]
                                            (get {"image/png"  0
                                                  "image/jpeg" 1
                                                  "image/gif"  2
                                                  "text/plain" 3} mime 10)))
                                 first)]
    (if (= "text/plain" mime)
      [body nil]
      [nil (save-attachment body mime)])))

(map email->relevant-content [{:body [{:content-type "TEXT/PLAIN; charset=UTF-8",
                                       :body         "text body\r\n"}
                                      {:content-type "TEXT/HTML; charset=UTF-8",
                                       :body         "<div>html body</div>\r\n"}
                                      {:content-type "IMAGE/PNG; charset=UTF-8",
                                       :body         (io/file "os.png")}]}
                              {:body [{:content-type "TEXT/PLAIN; charset=UTF-8",
                                       :body         "text body\r\n"}
                                      {:content-type "TEXT/HTML; charset=UTF-8",
                                       :body         "<div>html body</div>\r\n"}]}])

;; :to
;; :cc
;; :date-recieved
;; :bcc
;; :headers
;; :body
;; :sender
;; :id
;; :from
;; :subject
;; :date-sent
;; :multipart?
;; :content-type

(defn  email->message [msg]
  (let [reply-to     (get (apply merge (:headers msg)) "In-Reply-To")
        [text image-filename] (email->relevant-content msg)]
    (merge {:id      (-> msg :id first)
            :subject (:subject msg)
            :to      (-> msg :to first)
            :from    (:from msg)
            :image   image-filename
            :text    text}
           (when reply-to
             {:reply-to reply-to}))))


(defrecord EmailMessenger
  [imap smtp email password folder]
  Messenger
  (get-messages [{:keys [imap email password folder]}]
    (->> (mail/all-messages (mail/store imap email password) folder)
         (map clojure-mail.message/read-message)
         ;; (filter (fn [msg] (-> msg :to :name #{"Kitty Spielford"})))
         (map email->message)))

  (forward-message [{:keys [smtp email password]}
                    to
                    {:as message :keys [image text subject]}
                    opts]
    (println "Forwarding" subject "to" to "with" (if image image text))
    (postal/send-message {:host smtp
                          :user email
                          :pass password
                          :ssl  true}
                         {:user-agent "MyMailer 1.0"
                          :from    email
                          :to      to
                          :subject subject
                          :body    [(cond image {:type    :attachment
                                                 :content (io/file image)
                                                 :content-type (filename->mime image)}
                                          text  {:type "text/plain"
                                                 :content text})]} )))


