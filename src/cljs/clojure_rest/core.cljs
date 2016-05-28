(ns clojure-rest.core
  (:require [reagent.core :as reagent :refer [atom]]))


(enable-console-print!)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-card
  "Create a card to display in the dash-board"
  [title description status]
  {:title title
   :description description
   :status status})

(def card-list
  [(create-card "1st card" "This is my first description" :backlog)
   (create-card "2nd card" "This is my second description" :under-dev)
   (create-card "3nd card" "This is my third description" :under-dev)
   (create-card "4th card" "This is my fourth description" :done)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-card
  [card]
  [:div.card
   [:div.card-title (:title card)]
   [:div.card-description (:description card)]
  ])

(defn render-list
  [cards]
  [:div.list ; TODO - Try to give a name
   (for [c cards]
     [render-card c])
  ])

(defn render-board
  []
  (let [filter-status (fn [status] (filter #(= status (:status %)) card-list))]
    [:div.app
     [render-list (filter-status :backlog)]
     [render-list (filter-status :under-dev)]
     [render-list (filter-status :done)]
    ]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(reagent/render [render-board]
  (js/document.getElementById "app"))
