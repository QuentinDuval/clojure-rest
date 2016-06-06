(ns clojure-rest.api
  (:require
    [ajax.core :refer [GET POST]]
    [clojure-rest.utils :as utils]
  ))

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

(defn fetch-cards!
  [on-load]
  (GET "/cards"
    {:handler on-load
     :error-handler #(js/alert "Could not retrieve the cards")}
  ))

(defn create-task
  "Create a task to display in a card"
  [name done]
  {:name name
   :done done})

(defn add-task
  "Add a new task in the card"
  [card task]
  (update-in card [:tasks]
    conj (create-task task false)))

(defn remove-task-at
  "Remove a task at the provided index"
  [card task-index]
  (update-in card [:tasks] utils/remove-idx task-index))

