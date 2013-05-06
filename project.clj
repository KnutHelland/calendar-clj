(defproject khtools "0.1.0-SNAPSHOT"
  :description "ClojureScript Calendar by Knut Helland"
  :url ""
  :license {:name "Copyright (c) 2013 Knut Helland"}

  :source-paths ["src/cljs"]

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojurescript "0.0-1586"]
                 [jayq "2.3.0"]
                 [hiccups "0.2.0"]]

  :plugins [[lein-cljsbuild "0.3.0"]]
  :hooks [leiningen.cljsbuild]
  :min-lein-version "2.0.0"

  :cljsbuild
  {:repl-listen-port     9000
   :repl-launch-commands
   {"phantom"       ["phantomjs"
                     "phantom/repl.js"
                     :stdout ".repl-phantom-out"
                     :stderr ".repl-phantom-err"]

    "phantom-naked" ["phantomjs"
                     "phantom/repl.js"
                     "resources/private/html/naked.html"
                     :stdout ".repl-phantom-naked-out"
                     :stderr ".repl-phantom-naked-err"]}

   :test-commands        {"unit" ["phantomjs"
                                  "phantom/unit-test.js"
                                  "resources/private/html/unit-test.html"]}

   :builds               [ {:source-paths ["src/cljs"]
                            :id "dev"
                            :compiler    {:output-to     "resources/public/js/main-debug.js"
                                          :optimizations :simple
                                          :pretty-print  true}}
                           {:source-paths ["src/cljs"]
                            :id "prod"
                            :compiler    {:output-to     "resources/public/js/main.js"
                                          :optimizations :advanced
                                          :externs ["resources/externs/jquery-1.9.js"]
                                          :pretty-print  false}}
                           ;; {:source-paths ["test/cljs"]
                           ;;  :id "test"
                           ;;  :compiler    {:output-to     "resources/private/js/unit-test.js"
                           ;;                :optimizations :whitespace
                           ;;                :pretty-print  true}}
                           ]})
