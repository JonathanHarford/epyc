(require
 '[clojure.java.jdbc :as jdbc]
 '[clojure-mail.core :as mail]
 '[clojure.java.io :as io]
 'clojure-mail.message
 '[postal.core :as postal])
(def my-email-address "my@email.address")
(def smtp {:host "smtp.gmail.com" :user "GMAIL@ADDRE.SS" :pass "PASSWORD" :ssl true})
(def imap ["imap.gmail.com" (:user smtp) (:pass smtp)])

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

