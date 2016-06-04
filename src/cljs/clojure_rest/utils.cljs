(ns clojure-rest.utils
  (:require
    [clojure.string :as str]
  ))


(defn str-contains
  "Check whether the first string contains the second string"
  [stack needle]
  (< -1 (.indexOf stack needle)))

(defn lower-str-contains
  "Check whether the first string contains the second string"
  [stack needle]
  (str-contains (str/lower-case stack) (str/lower-case needle)))

(defn remove-idx
  "Remove the given index from the vector - but in linear time!"
  [v idx]
  (vec
    (concat (subvec v 0 idx) (subvec v (inc idx) (count v)))
  ))

(defn map-values
  "Like map but over the values of an associative container"
  [f coll]
  (into {}
    (map (fn [[k v]] [k (f v)]) coll)
  ))
