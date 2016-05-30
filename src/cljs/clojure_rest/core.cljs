(ns clojure-rest.core
  (:require-macros
    [reagent.ratom :refer [reaction]])
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
    (filter #(utils/str-contains (-> % second :title) title) cards)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; TODO - Separate concerns!
; Model at the end:
; - A map id to card
; - Collections for each status: each holding the list of IDs they have
; - Use a reaction to provide for the filtered list of cards (filtered by text)

(def app-state
  (r/atom
    {:cards {}
     :filter ""
    }))

(def card-list
  "FRP style zoom on the card-list to render"
  (reaction
    (filter-by-title (:filter @app-state) (:cards @app-state))
  ))

(defn update-filter!
  "Update the filter used in the app state"
  [filter]
  (swap! app-state #(assoc % :filter filter)))

(defn add-task-to!
  "Add a new task in the card"
  [card-id task]
  (swap! app-state
    #(update-in % [:cards card-id :tasks] conj (api/create-task! task false))
  ))

(defn remove-task-from!
  "Remove a task from a card"
  [card-id task-id]
  (defn filter-ids [tasks]
    (filterv #(not= (:task-id %) task-id) tasks))
  (swap! app-state #(update-in % [:cards card-id :tasks] filter-ids)
))

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
  (let [show-details (r/atom false) ; TODO - Extract show details in set of card-id
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
   (for [c (filter-by-status status (map second cards))]
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
  [:div
   [:input.search-input
    {:type "text"
     :placeholder "search"
     :value (:filter @app-state)
     :on-change #(update-filter! (.. % -target -value))}]
   [render-board @card-list]
   ])

(def fetch-and-render-app
  "Render the app - adding a fetching of data when the DOM is mounted"
  (let [add-card #(assoc-in %1 [:cards (:card-id %2)] %2)
        add-cards #(reduce add-card %1 %2)
        append-all (fn [cards] (swap! app-state #(add-cards % cards)))]
    (with-meta render-app
     {:component-did-mount #(fake/fake-fetch! append-all) })
  ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [fetch-and-render-app]
  (js/document.getElementById "app"))
