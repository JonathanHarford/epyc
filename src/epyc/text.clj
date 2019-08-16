(ns epyc.text)

(def start "I administer games of Eat Poop You Cat. Type `/play` to play, or `/help` for more information.")

(defn ->help [turns-per-game]
  (str
   "[Eat Poop You Cat](http://boardgamegeek.com/boardgame/30618/eat-poop-you-cat) "
   "(aka “Broken Picture Telephone”, “Telepictionary”, “The Caption Game”, "
   "“Doodle or Die”, “Drawception”, “Telestrations”) "
   "is a game of writing and drawing. The first player writes a sentence, the second player "
   "draws a picture based on the sentence, the third draws a picture based on "
   "the sentence, and so on until " turns-per-game " turns have been played. Type `/play` to play!"))

(def already-playing "You're already playing a turn!")
(def first-turn "You'll be starting off a new game. Please give me a sentence describing a scene. Be as detailed and strange as you like!")
(def turn-done "Thanks for playing! I'll message you when the game is done!")
(def request-text "Please describe the following picture. _Infer details! Decode a narrative!_")
(def request-photo "Please draw/sculpt/3d-craft the following scenario, and send me the picture. _No words! Light your picture well!_")
(def confused "I'm not sure what to do with that, since you're not in a game right now. Maybe try `/help`?")
(def game-done-1 "The game is finished! Here are all the turns:")
(def game-done-2 "Thanks for playing!")
(def pardon "Pardon?")
(def expect-text-got-photo "I was expecting a description from you, but you sent me a photo!")
(def expect-photo-got-text "I was expecting a photo from you, but you sent me a description!")
