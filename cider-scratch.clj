                                        ; CURRENT GOAL:
                                        ; Script that forwards each turn


(require
 '[clojure.java.jdbc :as jdbc]
 '[clojure-mail.core :as mail]
 '[clojure.java.io :as io]
 'clojure-mail.message
 '[postal.core :as postal])
(def my-email-address "my@email.address")
(def smtp {:host "smtp.gmail.com" :user "GMAIL@ADDRE.SS" :pass "PASSWORD" :ssl true})
(def imap ["imap.gmail.com" (:user smtp) (:pass smtp)])

;; DB
(def db-spec "postgresql://localhost:5432/epyc")
(jdbc/query db-spec ["SELECT * FROM game LIMIT 1"])

;; IMAP
(def store (apply mail/store imap))
(def msg-att (last (flatten (:body (clojure-mail.message/read-message (first (mail/all-messages store "attachement-test")))))))

;; WRITE TO FILE (WORKS)
(with-open [i (io/input-stream (:body msg-att))]
  (io/copy i (io/file "os.png")))

;; COPY FILE (WORKS)
(with-open [i (io/input-stream (io/file "os.png"))]
  (io/copy i (io/file "os2.png")))

;; so as far as io/copy is concerned:
;;(io/input-stream (:body msg-att))
;;is equivalent to
;;(io/input-stream (io/file "os.png"))


(with-open [i (io/input-stream (io/file "os.png"))
            o (io/output-stream (io/file "os3.png"))]
  (.write o i))

(seq (io/input-stream (io/file "os.png")))

(defn stream->bytes [stream]
  (with-open [xin (io/input-stream stream)
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    xout
    #_(.toByteArray xout)))

;;  No implementation of method: :as-url of protocol: #'clojure.java.io/Coercions
;;  found for class: com.sun.mail.util.BASE64DecoderStream
(postal/send-message
 smtp
 {:from    (:user smtp)
  :to      my-email-address
  :subject "goin postal"
  :body    [{:type "text/plain" :content "hey"}
            {:type         :attachment
             :content-type "image/png"
             :content      (:body msg-att)}]})

; No implementation of method: :as-url of protocol: #'clojure.java.io/Coercions found for class: java.io.BufferedInputStream
(postal/send-message
 smtp
 {:from    (:user smtp)
  :to      my-email-address
  :subject "goin postal"
  :body    [{:type "text/plain" :content "hey"}
            {:type         :attachment
             :content-type "image/png"
             :content      (io/input-stream (:body msg-att))}]})

;; WORKS, but how do we send a BASE64DecoderStream?
(postal/send-message
 smtp
 {:from    (:user smtp)
  :to      my-email-address
  :subject "goin postal"
  :body    [{:type "text/plain" :content "hey"}
            {:type         :attachment
             :content-type "image/png"
             :content      (stream->bytes (io/file "os.png"))}]})
