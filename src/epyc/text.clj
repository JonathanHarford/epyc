(ns epyc.text
  (:require [clojure.string :as str]))

(def start "I administer games of Eat Poop You Cat. Type `/play` to play, or `/help` for more information.")

(def help "[Eat Poop You Cat](http://boardgamegeek.com/boardgame/30618/eat-poop-you-cat)") ; TODO improve much

(def already-playing "You're already playing a turn!")  ; TODO resend last turn

(def first-turn "First turn!")
(def request-text "Gimme text")
(def request-photo "Gimme photo")
(def confused "I'm confused.")
