(ns repl
  (:require
    [clojure.browser.repl :as brepl]))

(defn ^:export connect [ ]
  (brepl/connect "http://localhost:9000/repl"))