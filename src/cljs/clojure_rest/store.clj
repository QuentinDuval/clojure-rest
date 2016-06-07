(ns clojure-rest.store)


(defmacro def-multi-reducer
  "Create a multi reducer named with 'name' and followed by map of keywords to functions"
  [name & reducer-mapping]
 `(def ~name
    (clojure-rest.store/multi-reducer
      ~(into {} (map vec) (partition 2 reducer-mapping))
      )
  ))
