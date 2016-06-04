(ns clojure-rest.utils)


(defn str-contains
  "Check whether the first string contains the second string"
  [stack needle]
  (< -1 (.indexOf stack needle)))

(defn remove-idx
  "Remove the given index from the vector - but in linear time!"
  [v idx]
  (vec
    (concat (subvec v 0 idx) (subvec v (inc idx) (count v)))
  ))

(defn map-values
  [f coll]
  (into {}
    (map (fn [[k v]] [k (f v)]) coll)
  ))
