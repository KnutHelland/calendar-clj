(ns ^{:doc    "Feature1 tests"
      :author ""}
  test.feature1)

(defn- test1 [ ]
  (.log js/console " feature1.test1")
  (assert (= 1 1)))

(defn run [ ]
  (test1))
