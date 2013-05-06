(ns ^{:doc    "tests for your awesome library"
      :author ""}
  test
  (:require [test.feature1 :as f1]))

(def success 0)

(defn ^:export run []
  (.log js/console "Add your tests.")
  (f1/run)
  success)
