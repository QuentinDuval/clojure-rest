(ns clojure-rest.card)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; The available statuses
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def BACKLOG "Backlog")
(def DONE "Done")
(def UNDER-DEV "In progress")


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; The available categories
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def BUG-FIX "bug-fix")
(def ENHANCEMENT "enhancement")


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce ^:private
  next-card-id (atom 0))

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

