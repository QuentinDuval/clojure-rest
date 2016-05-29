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

(defn status->str
  [status]
  (condp = status
    :backlog "Backlog"
    :under-dev "Under dev"
    :done "Done"
    :else "Unknown"))

(defn filter-by-status
  [status cards]
  (filter #(= status (:status %)) cards))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn render-task
  [task]
  [:li.checklist__task
   [:input {:type "checkbox" :default-checked (:done task)}]
   (:name task)
   [:a.checklist__task--remove {:href "#"}]
  ])

(defn render-tasks
  [tasks]
  [:div.checklist
   [:ul (map render-task tasks)]
  ])

(defn render-card
  []
  (let [is-show-details (r/atom false)
        show-details #(swap! is-show-details not)]
    (fn [card]
      [:div.card
       [:div.card__title {:on-click show-details} (:title card)]
       (when @is-show-details
         [:div ;TODO - If there is not div here, it does not work
          [:div.card__details (:description card)]
          [render-tasks (:tasks card)]])
      ])
    ))

(defn render-list
  [status cards]
  [:div.list
   [:h1 (status->str status)]
   (for [c (filter-by-status status cards)]
     [render-card c])
   ; (map render-card (filter-by-status status cards))
   ; TODO - Investigate why if you use map it does not work...
  ])

(defn render-board
  [cards]
  [:div.app
   (for [status [:backlog :under-dev :done]]
     [render-list status cards])
  ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(r/render [render-board card-list]
  (js/document.getElementById "app"))
