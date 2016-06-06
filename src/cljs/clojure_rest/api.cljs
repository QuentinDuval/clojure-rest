(ns clojure-rest.api
  (:require
    [ajax.core :refer [GET POST]]
    [clojure-rest.card :as card]
    [clojure-rest.utils :as utils]
  ))

(defn fetch-cards!
  [on-load]
  (GET "/cards"
    {:handler #(on-load (:cards %))
     :response-format :json
     :keywords? true
     :error-handler #(js/alert (str "Could not retrieve the cards: " %))}
  ))

(defn add-task
  "Add a new task in the card"
  [card task]
  (update-in card [:tasks]
    conj (card/create-task task false)))

(defn remove-task-at
  "Remove a task at the provided index"
  [card task-index]
  (update-in card [:tasks] utils/remove-idx task-index))

