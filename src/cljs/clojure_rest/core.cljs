(ns clojure-rest.core
  (:require-macros
    [clojure-rest.store :refer [def-multi-reducer]]
    [reagent.ratom :refer [reaction]])
  (:require
    [clojure-rest.api :as api]
    [clojure-rest.card :as card :refer [BACKLOG BUG-FIX DONE ENHANCEMENT UNDER-DEV]]
    [clojure-rest.store :as store]
    [clojure-rest.utils :as utils]
    [clojure-rest.comp.card :as card-view]
    [clojure-rest.comp.search :as search]
    [reagent.core :as r]
  ))

; https://github.com/reagent-project/reagent/blob/master/src/reagent/core.cljs
; https://github.com/reagent-project/reagent-cookbook/blob/master/old-recipes/nvd3/README.md
; https://github.com/Day8/re-frame/wiki/When-do-components-update%3F
; https://github.com/gf3/secretary
; https://github.com/JulianBirch/cljs-ajax
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def app-state
  (r/atom {:cards {}}))

(defn add-cards
  "Add several cards to the application state"
  [store cards]
  ; BUG - Keeping the :show-details at app-state means that rendering twice
  ; the component would lead to the GUI being updated at two places
  (let [default-state {:show-details false}
        to-gui-card #(vector (:card-id %) (merge % default-state))]
    (merge store (map to-gui-card cards))
  ))

(defn toggle-all-cards
  [cards]
  (let [all-toggled (every? #(-> % second :show-details) cards)
        toggle-card #(assoc % :show-details (not all-toggled))]
    (utils/map-values toggle-card cards)
  ))

(defn filter-by-title
  "Keep only cards that contains the searched string inside their title"
  [title cards]
  (if (empty? title)
    cards
    (filter #(utils/lower-str-contains (-> % second :title) title) cards)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def-multi-reducer
  app-reducer
  :initialize (fn [store cards] (add-cards store cards))
  :on-add-card (fn [store _] (js/alert "TODO - Add via server") store)
  :on-toggle-all (fn [store _] (toggle-all-cards store))
  :on-toggle-card (fn [store card-id]
                    (update-in store [card-id :show-details] not))
  :on-remove-task (fn [store card-id task-id]
                    (update-in store [card-id] api/remove-task-at task-id))
  :on-check-task (fn [store card-id task-id]
                    (update-in store [card-id :tasks task-id :done] not))
  :on-add-task (fn [store card-id task]
                 (update-in store [card-id] api/add-task task))
  :on-card-drop (fn [cards card-id status]
                  (assoc-in cards [card-id :status] status))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn status->str
  [status]
  (condp = status
    BACKLOG "Backlog"
    UNDER-DEV "In Progress"
    DONE "Done"))

(defn render-column
  "[Pure] Render a column holding a set of cards" 
  [card-renderer dispatch]
  (fn [status cards]
    [:div.column
     {:onDragOver #(.preventDefault %)
      :onDrop #(dispatch :on-card-drop (utils/get-transfer-data % :card-id int) status)}
     [:h1 (status->str status)]
     (for [c cards] ^{:key (:card-id c)} [card-renderer c])
    ]))

(defn render-board
  "[Pure] Render the dash-board as a set of column (one by status)"
  [column-rendered]
  (fn [cards]
    (let [cards-by-status (group-by :status (map second cards))]
      [:div.board
       (for [status [BACKLOG UNDER-DEV DONE]]
         ^{:key status} [column-rendered status (cards-by-status status)])
      ])
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-toggle-all
  "[Pure] Render the button to expand all cards" 
  [dispatch]
  (fn []
    [:button.header-button
     {:type "button" :on-click #(dispatch :on-toggle-all)}
     "Expand all"]
  ))

(defn render-add-card
  "[Pure] Render the button to expand add a new card" 
  [dispatch]
  (fn []
    [:button.header-button
     {:on-click #(dispatch :on-add-card) :type "button"}
     "Add card"]
  ))

(defn render-app
  "[Stateful] Render the application" 
  [dispatch]
  (let [filter (r/atom "")
        card-renderer (card-view/render-card dispatch)
        column-rendered (render-column card-renderer dispatch)
        board-renderer (render-board column-rendered)]
    (fn [cards]
      [:div
       [search/render-filter filter]
       [render-add-card dispatch]
       [render-toggle-all dispatch]
       [board-renderer (filter-by-title @filter cards)]
      ])
    ))

(defn fetch-and-render-app
  "Render the app - adding a fetching of data when the DOM is mounted"
  []
  (let [cards (r/cursor app-state [:cards])
        card-store (store/reducer-store cards app-reducer)
        dispatch (store/dispatcher card-store)
        app-render (render-app dispatch)]
    (r/create-class
      {:component-did-mount #(api/fetch-cards! (partial dispatch :initialize))
       :reagent-render (fn [] [app-render @cards])})
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [fetch-and-render-app]
  (js/document.getElementById "app"))
