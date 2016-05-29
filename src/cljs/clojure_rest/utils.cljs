(ns clojure-rest.utils)


(defn str-contains
  "Check whether the first string contains the second string"
  [stack needle]
  (< -1 (.indexOf stack needle)))
