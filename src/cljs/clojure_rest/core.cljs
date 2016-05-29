(ns clojure-rest.core
  (:require [reagent.core :as r]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-task
  [name done]
  {:name name
   :done done})

(defn create-card
  "Create a card to display in the dash-board"
  [title description category status & tasks]
  {:title title
   :description description
   :category category
   :status status
   :tasks (into [] tasks)})

(defn status->str
  [status]
  (condp = status
    :backlog "Backlog"
    :under-dev "Under dev"
    :done "Done"
    :else "Unknown"))

(defn category->color
  [category]
  (condp = category
    :bug-fix "#BD8D31"
    :enhancement "#3A7E28"
    :else "#eee"))

(defn filter-by-status
  [status cards]
  (filter #(= status (:status %)) cards))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def card-list
  (r/atom
    [(create-card
       "1st card"
       "This is my first description"
       :bug-fix
       :backlog
       (create-task "Done some stuff" true))
     (create-card
       "2nd card"
       "This is my second description"
       :enhancement
       :under-dev
       (create-task "Done some stuff" true)
       (create-task "Done some more stuff" false))
     (create-card
       "3nd card"
       "This is my third description"
       :bug-fix
       :under-dev)
     (create-card
       "4th card"
       "This is my fourth description"
       :enhancement
       :done)]))

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
   [:ul (for [t tasks] [render-task t])]
  ])

(defn card-side-color
  [card]
  {:position "absolute" :zindex -1 :top 0 :bottom 0 :left 0 :width 7
   :backgroundColor (-> card :category category->color)
  })

(defn render-card
  []
  (let [show-details (r/atom false)
        toggle-details #(swap! show-details not)
        title-style #(if % :div.card__title--is-open :div.card__title)]
    (fn [card]
      [:div.card
       [:div {:style (card-side-color card)}]
       [(title-style @show-details) {:on-click toggle-details} (:title card)]
       (when @show-details
         [:div.card__details
          (:description card)
          [render-tasks (:tasks card)]
         ])
       ])
    ))

(defn render-list
  [status cards]
  [:div.list
   [:h1 (status->str status)]
   (for [c (filter-by-status status cards)]
     [render-card c])
  ])

(defn render-board
  [cards]
  [:div.app
   (for [status [:backlog :under-dev :done]]
     [render-list status cards])
  ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(r/render [render-board @card-list]
  (js/document.getElementById "app"))
