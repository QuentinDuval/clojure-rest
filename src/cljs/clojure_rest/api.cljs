(ns clojure-rest.api)


(defonce next-task-id (atom 0))
(defonce next-card-id (atom 0))

(defn create-task!
  "Create a task to display in a card"
  [name done]
  (swap! next-task-id inc)
  {:task-id @next-task-id
   :name name
   :done done})

(defn create-card!
  "Create a card to display in the dash-board"
  [title description category status & tasks]
  (swap! next-card-id inc)
  {:card-id @next-card-id
   :title title
   :description description
   :category category
   :status status
   :tasks (into [] tasks)})

