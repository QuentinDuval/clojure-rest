(ns clojure-rest.core
  (:require
    [reagent.core :as r]
    [clojure-rest.api :as api]
    [clojure-rest.fake-data :as fake]
    [clojure-rest.utils :as utils]
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn status->str
  [status]
  (condp = status
    :backlog "Backlog"
    :under-dev "In Progress"
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
    (filter #(utils/str-contains (:title %) title) cards)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; TODO - Separate concerns:
; - cards values and their id, and the order in which they appear
; - tasks values and their id
; - if you want to expand all cards, you need the state to be public

(def card-list (r/atom []))

(defn add-task-to!
  "Add a new task in the card"
  [card-id task]
  (defn add-if! [card]
    (if (= card-id (:card-id card))
      (update-in card [:tasks] conj (api/create-task! task false))
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
  (let [append-all (fn [cards] (swap! card-list #(into % cards)))]
    (with-meta render-app
     {:component-did-mount #(fake/fake-fetch! append-all) })
  ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [fetch-and-render-app]
  (js/document.getElementById "app"))
