(ns clojure-rest.card)


; TO SHARE CODE
; https://github.com/emezeske/lein-cljsbuild/blob/master/doc/CROSSOVERS.md


(defonce next-card-id (atom 0))

(defn create-card!
  "Create a card to display in the dash-board"
  [title description category status & tasks]
  (swap! next-card-id inc)
  {:card-id @next-card-id
   :title title
   :description description
   :category category
   :status status
   :tasks (vec tasks)})

(defn create-task
  "Create a task to display in a card"
  [name done]
  {:name name
   :done done})

