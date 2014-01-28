(defproject copious/auth "0.3.1-SNAPSHOT"
  :description "clojure api for the auth model stored in riak"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.google.protobuf/protobuf-java "2.4.1"]
                 [com.novemberain/welle "1.2.0"]]
  :plugins [[utahstreetlabs/lein-protobuf "0.2.0-SNAPSHOT"]
            [lein-release "1.0.0"]
            [lein-midje "2.0.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}}

  :prep-tasks [["protobuf" "compile"]]
  :repositories {"snapshots"
                 {:url "" :username "" :password ""}
                 "releases"
                 {:url "" :username "" :password ""}}

  :protobuf-version "2.4.1")
