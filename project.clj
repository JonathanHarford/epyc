(defproject epyc "0.1.0-SNAPSHOT"
  :description "Administers games of Eat Poop You Cat over Telegram"
  :url "https://github.com/JonathanHarford/epyc-email"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [environ "1.1.0"]
                 [morse "0.4.2"]
                 [postgresql "9.3-1102.jdbc41"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [com.taoensso/timbre "4.10.0"]]
  :main ^:skip-aot epyc.core
  :uberjar-name "epyc-standalone.jar"
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
