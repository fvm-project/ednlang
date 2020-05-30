(defproject fvm-project/ednlang "0.1.0-SNAPSHOT"
  :description "ednlang is a simple stack-based concatenative language on top of fvm"
  :url "https://github.com/fvm-project/ednlang"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 [fvm-project/fvm "0.1.0"]]
  :main ednlang.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:source-paths ["src" "dev"]
                   :dependencies [[com.clojure-goes-fast/clj-async-profiler "0.4.1"]]
                   :jvm-opts ["-Djdk.attach.allowAttachSelf"]}}
  :plugins [[lein-shell "0.5.0"]
            [lein-eftest "0.5.9"]
            [lein-cloverage "1.1.2"]]
  :aliases
  {"native"
   ["shell"
    "native-image"
    "--no-fallback"
    "--allow-incomplete-classpath"
    "--report-unsupported-elements-at-runtime"
    "--initialize-at-build-time"
    "-jar" "./target/uberjar/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
    "-H:+ReportExceptionStackTraces"
    "-H:Name=./target/${:name}"]

   "profile" ["run" "-m" "profiler"]})
