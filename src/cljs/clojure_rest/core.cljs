(ns clojure-rest.core
  (:require [reagent.core :as r]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn str-contains
  "Check whether the first string contains the second string"
  [stack needle]
  (< -1 (.indexOf stack needle)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn filter-by-title
  "Keep only cards that contains the searched string inside their title"
  [title cards]
  (if (empty? title)
    cards
    (filter #(str-contains (:title %) title) cards)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; TODO - Separate concerns:
; - cards values and their id, and the order in which they appear
; - tasks values and their id

(def card-list (r/atom []))

(defn add-task-to!
  "Add a new task in the card"
  [card-id task]
  (defn add-if! [card]
    (if (= card-id (:card-id card))
      (update-in card [:tasks] conj (create-task! task false))
      card))
  (swap! card-list #(mapv add-if! %)))

(defn remove-task-from!
  "Remove a task from a card"
  [card-id task-id]
  (defn remove-if! [card]
    (if (= card-id (:card-id card))
      (update-in card [:tasks]
        (fn [tasks]
          (filterv #(not= (:task-id %) task-id) tasks)))
      card))
  (swap! card-list #(mapv remove-if! %)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def init-card-list
  "Fake card list returned by the server"
  [(create-card!
     "1st card"
     "This is my first description"
     :bug-fix
     :backlog
     (create-task! "Done some stuff" true))
   (create-card!
     "2nd card"
     "This is my second description"
     :enhancement
     :under-dev
     (create-task! "Done some stuff" true)
     (create-task! "Done some more stuff" false))
   (create-card!
     "3rd card"
     "This is my third description"
     :bug-fix
     :under-dev)
   (create-card!
     "4th card"
     "This is my fourth description"
     :enhancement
     :done)])

(defn fake-fetch
  []
  (js/setTimeout
    #(swap! card-list
       (fn [cards] (into cards init-card-list)))
    1000))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-task
  [card-id task]
  [:li.checklist__task
   [:input {:type "checkbox" :default-checked (:done task)}]
   (:name task)
   [:a.checklist__task--remove
    {:href "#" :on-click #(remove-task-from! card-id (:task-id task))}]
  ])

(defn render-tasks
  [card-id tasks]
  [:div.checklist
   [:ul
    (for [t tasks]
      ^{:key (:task-id t)} [render-task card-id t])]
  ])

(defn card-side-color
  "Render the ribbon on the left of the card, that indicates its category"
  [card]
  {:position "absolute" :zindex -1 :top 0 :bottom 0 :left 0 :width 5
   :backgroundColor (-> card :category category->color)
  })

(defn render-add-list
  "Render the text field allowing to add new tasks to a card"
  [card-id]
  [:input.checklist--add-task
   {:type "text"
    :placeholder "Type then hit Enter to add a task"
    :on-key-press (fn [e]
                    (when (= "Enter" (.-key e))
                      (add-task-to! card-id (.. e -target -value))
                      (set! (.. e -target -value) "")
                  ))
    }])

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
          [render-tasks (:card-id card) (:tasks card)]
          [render-add-list (:card-id card)]
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
        on-search-enter #(reset! search-text (.. % -target -value))]
    (fn []
      [:div
       [:input.search-input
        {:type "text"
         :placeholder "search"
         :value @search-text
         :on-change on-search-enter}]
       [render-board (filter-by-title @search-text @card-list)]
      ])
    ))

(def fetch-and-render-app
  "Render the app - adding a fetching of data when the DOM is mounted"
  (with-meta render-app
    {:component-did-mount fake-fetch}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [fetch-and-render-app]
  (js/document.getElementById "app"))
