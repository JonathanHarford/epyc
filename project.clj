(defproject epyc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [environ "1.1.0"]
                 [morse "0.4.1"]
                 [postgresql "9.3-1102.jdbc41"]
                 [org.clojure/java.jdbc "0.7.9"]]
  :main ^:skip-aot epyc.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
