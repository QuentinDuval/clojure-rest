(ns clojure-rest.store)

(defmacro def-multi-reducer
  [name reducer-map]
 `(def ~name
    (clojure-rest.store/multi-reducer ~reducer-map)
  ))
