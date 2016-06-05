(ns clojure-rest.store
  (:require
    [cljs.core.async :refer [put! chan <!]]
    [reagent.core :as r])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]])
  )


(defn reducer-store
  "Create a generic appplication store, responsible for a state and mutating it via a reducer"
  [store-ref reducer]
  (let [input (chan)]
    (go-loop []
      (let [msg (<! input)]
        (swap! store-ref reducer msg)
        (recur)))
    input
  ))

(defn multi-reducer
  "Create a reducer out of several reducers that dispatch messages to the responsible ones
   Assumes that the first argument of the message is the key on which to dispatch"
  [reducer-map]
  (fn [store [msg args]]
    (when-let [reducer (get reducer-map msg)]
      (apply reducer store args))
    ))

(defn dispatcher
  "Create a function to dispatch a message to a store"
  [store-chan]
  (fn [msg & args]
    (put! store-chan [msg (vec args)])
    ))


;;;;;;;;;;;;;;;;;;; TEST CODE ;;;;;;;;;;;;;;;;;;;

(def ^:private counter
  (r/atom 1))

(def ^:private counter-handlers
  (multi-reducer
    {::sum +
     ::mul *
  }))

(def ^:private counter-store
  (reducer-store counter counter-handlers))

(def ^:private counter-dispatch
  (dispatcher counter-store))

(defn test-counter
  []
  (counter-dispatch ::sum 10)
  (counter-dispatch ::mul 2)
  @counter)




