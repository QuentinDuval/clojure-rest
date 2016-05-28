(ns clojure-rest.core
  (:require [reagent.core :as r]))


(enable-console-print!)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-task
  [name done]
  {:name name
   :done done})

(defn create-card
  "Create a card to display in the dash-board"
  [title description status & tasks]
  {:title title
   :description description
   :status status
   :tasks (into [] tasks)})

(def card-list
  [(create-card
     "1st card"
     "This is my first description"
     :backlog)
   (create-card
     "2nd card"
     "This is my second description"
     :under-dev
     (create-task "Done some stuff" true)
     (create-task "Done some more stuff" false))
   (create-card
     "3nd card"
     "This is my third description"
     :under-dev)
   (create-card
     "4th card"
     "This is my fourth description"
     :done)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-tasks
  [tasks]
  [:div.checklist
   [:ul
   (for [t tasks]
     [:li.checklist__task
      [:input {:type "checkbox" :default-checked (:done t)}]
      (:name t)
      [:a.checklist__task--remove {:href "#"}]
     ])
  ]])

(defn render-card
  [card]
  [:div.card
   [:div.card__title (:title card)]
   [:div.card__details (:description card)]
   [render-tasks (:tasks card)]
  ])

(defn render-list
  [title cards]
  [:div.list
   [:h1 title]
   (for [c cards]
     [render-card c])
  ])

(defn render-board
  []
  (let [filter-status (fn [status] (filter #(= status (:status %)) card-list))]
    [:div.app
     [render-list "Backlog" (filter-status :backlog)]
     [render-list "Under dev" (filter-status :under-dev)]
     [render-list "Done" (filter-status :done)]
    ]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(r/render [render-board]
  (js/document.getElementById "app"))
