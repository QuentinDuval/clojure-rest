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

(defn card-side-color
  "Render the ribbon on the left of the card, that indicates its category"
  [card]
  {:position "absolute" :zindex -1 :top 0 :bottom 0 :left 0 :width 5
   :backgroundColor (-> card :category category->color)
  })

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

(def app-state
  (r/atom
    {:cards {}
     :filter ""
    }))

(def card-list
  "FRP style zoom on the app state to render"
  (reaction
    (filter-by-title (:filter @app-state) (:cards @app-state))
  ))

(defn update-card!
  "Update a card"
  [card]
  (swap! app-state
    #(assoc-in % [:cards (:card-id card)] card)
  ))

(defn add-cards!
  "Add several cards to the application state"
  [cards]
  (let [to-id-pair #(vector (:card-id %) %)]
    (swap! app-state
     #(update-in % [:cards] merge (map to-id-pair cards))
  )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-task
  [on-remove on-check task]
  [:li.checklist__task
   [:input {:type "checkbox"
            :default-checked (:done task)
            :on-click on-check}]
   (:name task)
   [:a.checklist__task--remove {:href "#" :on-click on-remove}]
  ])

(defn render-tasks
  [{:keys [tasks] :as card}]
  (let [on-remove #(update-card! (api/remove-task-at card %))
        on-check #(update-card! (update-in card [:tasks % :done] not))]
    [:div.checklist
     [:ul
      (map
        (fn [idx t]
          ^{:key t} [render-task #(on-remove idx) #(on-check idx) t])
        (range) tasks)]
     ]))

(defn render-add-task
  "Render the text field allowing to add new tasks to a card"
  [card]
  [:input.checklist--add-task
   {:type "text"
    :placeholder "Type then hit Enter to add a task"
    :on-key-press
    (fn [e]
      (when (= "Enter" (.-key e))
        (update-card! (api/add-task card (.. e -target -value)))
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
          [render-tasks card]
          [render-add-task card]
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
     ^{:key status} [render-list status cards])
  ])

(defn render-filter
  [filter-cursor]
  [:input.search-input
    {:type "text" :placeholder "search"
     :value @filter-cursor
     :on-change #(reset! filter-cursor (.. % -target -value))}
  ])

(defn render-app
  []
  (let [filter (r/cursor app-state [:filter])]
    [:div
     (render-filter filter)
     [render-board @card-list]]
  ))

(def fetch-and-render-app
  "Render the app - adding a fetching of data when the DOM is mounted"
  (with-meta render-app
    {:component-did-mount #(fake/fake-fetch! add-cards!)}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [fetch-and-render-app]
  (js/document.getElementById "app"))
