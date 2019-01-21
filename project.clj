(defproject soil "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [leoiacovini/clj-service "1.3.0"]
                 [org.clojure/core.async "0.4.474"]
                 [selmer "1.11.9"]
                 [com.ibm.etcd/etcd-java "0.0.5"]
                 [nubank/kubernetes-api "1.4.0"]
                 [clj-http "3.9.0"]
                 [http-kit "2.3.0"]
                 [me.raynes/conch "0.8.0"]
                 [cheshire "5.8.0"]
                 [formicarium/clj-json-patch "0.1.9"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :test-paths ["test/"]
  :repositories [["central" {:url "https://repo1.maven.org/maven2/" :snapshots false}]
                 ["clojars" {:url "https://clojars.org/repo/"}]]

  :profiles {:dev     {:aliases      {"run-dev" ["trampoline" "run" "-m" "soil.server/run-dev"]}
                       :plugins      [[lein-midje "3.2.1"]]
                       :dependencies [[midje "1.9.1"]
                                      [clj-http-fake "1.0.3"]
                                      [http-kit.fake "0.2.1"]
                                      [nubank/matcher-combinators "0.5.0"]
                                      [nubank/selvage "0.0.1"]]}
             :uberjar {:aot [soil.server]}}
  :main ^{:skip-aot true} soil.server)

