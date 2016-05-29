(ns clojure-rest.core
  (:require [reagent.core :as r]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-task
  "Create a task to display in a card"
  [id name done]
  {:task-id id
   :name name
   :done done})

(defn create-card
  "Create a card to display in the dash-board"
  [id title description category status & tasks]
  {:card-id id
   :title title
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

(defn str-contains
  "Check whether the first string contains the second string"
  [stack needle]
  (< -1 (.indexOf stack needle)))

(defn filter-by-title
  "Keep only cards that contains the searched string inside their title"
  [title cards]
  (if (empty? title)
    cards
    (filter #(str-contains (:title %) title) cards)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def card-list
  (r/atom
    [(create-card
       1 "1st card"
       "This is my first description"
       :bug-fix
       :backlog
       (create-task 1 "Done some stuff" true))
     (create-card
       2 "2nd card"
       "This is my second description"
       :enhancement
       :under-dev
       (create-task 2 "Done some stuff" true)
       (create-task 3 "Done some more stuff" false))
     (create-card
       3 "3rd card"
       "This is my third description"
       :bug-fix
       :under-dev)
     (create-card
       4 "4th card"
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
   [:ul
    (for [t tasks]
      ^{:key (:task-id t)} [render-task t])]
   [:input.checklist--add-task
    {:type "text" :placeholder "Type then hit Enter to add a task"}]
  ])

(defn card-side-color
  [card]
  {:position "absolute" :zindex -1 :top 0 :bottom 0 :left 0 :width 5
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
     ^{:key (:card-id c)} [render-card c])
  ])

(defn render-board
  [cards]
  [:div.app
   (for [status [:backlog :under-dev :done]]
     [render-list status cards])
  ])

(defn render-app
  []
  (let [search-text (r/atom "")
        on-search-enter (fn [e] (reset! search-text (.. e -target -value)))]
    (fn []
      [:div
       [:input.search-input
        {:type "text" :placeholder "search"
         :value @search-text :on-change on-search-enter}]
       [render-board (filter-by-title @search-text @card-list)]])
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [render-app] (js/document.getElementById "app"))
