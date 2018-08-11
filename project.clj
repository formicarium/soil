(defproject soil "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [leoiacovini/clj-service "1.1.6"]
                 [org.clojure/core.async "0.4.474"]
                 ;[ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 ;[org.slf4j/jul-to-slf4j "1.7.25"]
                 ;[org.slf4j/jcl-over-slf4j "1.7.25"]
                 ;[org.slf4j/log4j-over-slf4j "1.7.25"]
                 [nubank/kubernetes-api "1.4.0"]
                 [clj-http "3.9.0"]
                 [http-kit "2.3.0"]
                 [me.raynes/conch "0.8.0"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :test-paths ["test/" "selvage/"]
  :repositories [["central" {:url "https://repo1.maven.org/maven2/" :snapshots false}]
                 ["clojars" {:url "https://clojars.org/repo/"}]]

  :profiles {:dev     {:aliases      {"run-dev" ["trampoline" "run" "-m" "soil.server/run-dev"]}
                       :plugins      [[lein-midje "3.2.1"]]
                       :dependencies [[midje "1.9.1"]
                                      [clj-http-fake "1.0.3"]
                                      [http-kit.fake "0.2.1"]
                                      [nubank/selvage "0.0.1"]]}
             :uberjar {:aot [soil.server]}}
  :main ^{:skip-aot true} soil.server)

