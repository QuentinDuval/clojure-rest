(ns clojure-rest.utils
  (:require
    [clojure.string :as str]
  ))


(defn lower-str-contains
  "Check whether the first string contains the second string"
  [stack needle]
  (str/includes? (str/lower-case stack) (str/lower-case needle)))

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

(defn set-transfer-data
  "Set the transfer data inside a drag start event"
  [event key val]
  (.setData (.. event -nativeEvent -dataTransfer) key val))

(defn get-transfer-data
  "Get the transfer data inside a drag drop event"
  ([event key]
    (.getData (.. event -nativeEvent -dataTransfer) key))
  ([event key cast-fct]
    (cast-fct (get-transfer-data event key)))
  )
