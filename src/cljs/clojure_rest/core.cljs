(ns clojure-rest.core
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [reagent.core :as r]
    [clojure-rest.api :as api]
    [clojure-rest.fake-data :as fake]
    [clojure-rest.utils :as utils]
  ))

; https://github.com/reagent-project/reagent/blob/master/src/reagent/core.cljs
; https://github.com/reagent-project/reagent-cookbook/blob/master/old-recipes/nvd3/README.md
; https://github.com/Day8/re-frame/wiki/When-do-components-update%3F
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn status->str
  [status]
  (case status
    :backlog "Backlog"
    :under-dev "In Progress"
    :done "Done"
    :else "Unknown"))

(defn category->color
  [category]
  (case category
    :bug-fix "#BD8D31"
    :enhancement "#3A7E28"
    :else "#eee"))

(defn card-side-color
  "Render the ribbon on the left of the card, that indicates its category"
  [card]
  {:position "absolute" :zindex -1 :top 0 :bottom 0 :left 0 :width 5
   :backgroundColor (-> card :category category->color)
  })

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def app-state
  (r/atom {:cards {}}))

(defn add-cards!
  "Add several cards to the application state"
  [cards]
  ; BUG - Keeping the :show-details at app-state means that rendering twice
  ; the component would lead to the GUI being updated at two places
  ; => It cannot be in the component alone (if you want the expand all)
  ;    But it should be in a state specific to the board (assoc container)
  (let [default-state {:show-details false}
        to-gui-card #(vector (:card-id %) (merge % default-state))]
    (swap! app-state
     #(update-in % [:cards] merge (map to-gui-card cards))
  )))

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

(defn render-task
  "[Pure] display a task you can check or delete" 
  [task on-remove on-check]
  [:li.checklist__task
   [:input {:type "checkbox"
            :default-checked (:done task)
            :on-click on-check}]
   (:name task)
   [:a.checklist__task--remove {:href "#" :on-click on-remove}]
  ])

(defn render-tasks
  "[Pure] Display a list of tasks you can check or remove" 
  [tasks on-remove on-check]
  [:div.checklist
   [:ul
    (map-indexed
      (fn [idx t]
        ^{:key t} [render-task t #(on-remove idx) #(on-check idx)])
      tasks
    )]
  ])

(defn render-add-task
  "[Pure] Render the text field to add new tasks to a card"
  [on-add]
  [:input.checklist--add-task
   {:type "text"
    :placeholder "Type then hit Enter to add a task"
    :on-key-press
    (fn [e]
      (when (= "Enter" (.-key e))
        (on-add (.. e -target -value)) 
        (set! (.. e -target -value) "")
      ))
   }])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-drop
  [cards e status]
  (let [card-id (utils/get-transfer-data e :card-id int)]
    (assoc-in cards [card-id :status] status)
  ))

(defn event-handlers
  [cards]
  {:on-add-card #(js/alert "toto - use route to display the form")
   :on-toggle-all #(swap! cards toggle-all-cards)
   :on-toggle-card #(swap! cards update-in [%1 :show-details] not)
   :on-remove-task #(swap! cards update-in [%1] api/remove-task-at %2)
   :on-check-task #(swap! cards update-in [%1 :tasks %2 :done] not)
   :on-add-task #(swap! cards update-in [%1] api/add-task %2)
   :on-card-drop #(swap! cards handle-drop %1 %2)
  })

(defn render-card
  ; TODO - Try to remove the mess of call-backs
  "[Pure] Render a card" 
  [{:keys [on-toggle-card on-remove-task on-check-task on-add-task]
    :as event-handlers}]
  (fn [{:keys [card-id title description show-details tasks]
        :as card}]
    (let [title-style (if show-details :div.card__title--is-open :div.card__title)
          on-drag-start #(utils/set-transfer-data % :card-id card-id)]
      [:div.card
       {:draggable true :onDragStart on-drag-start}
       [:div {:style (card-side-color card)}]
       [title-style {:on-click #(on-toggle-card card-id)} title]
       [:div.card__details
        (when-not show-details {:style {:display "none"}})
        description
        [render-tasks tasks #(on-remove-task card-id %) #(on-check-task card-id %)]
        [render-add-task #(on-add-task card-id %)]
      ]]
  )))

(defn render-column
  "[Pure] Render a column holding a set of cards" 
  [card-renderer on-card-drop]
  (fn [status cards]
    [:div.column
     {:onDragOver #(.preventDefault %)
      :onDrop #(on-card-drop % status)}
     [:h1 (status->str status)]
     (for [c cards] ^{:key (:card-id c)} [card-renderer c])
    ]))

(defn render-board
  "[Pure] Render the dash-board as a set of column (one by status)"
  [column-rendered]
  (fn [cards]
    (let [cards-by-status (group-by :status (map second cards))]
      [:div.board
       (for [status [:backlog :under-dev :done]]
         ^{:key status} [column-rendered status (cards-by-status status)])
      ])
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-filter
  "[Side effect] Render the filter to only show cards containing a given text" 
  [filter-ref]
  [:input.search-input
    {:type "text" :placeholder "search"
     :value @filter-ref
     :on-change #(reset! filter-ref (.. % -target -value))}
  ])

(defn render-toggle-all
  "[Pure] Render the button to expand all cards" 
  [on-toggle-all]
  (fn []
    [:button.header-button
     {:type "button" :on-click on-toggle-all} "Expand all"]
  ))

(defn render-add-card
  "[Pure] Render the button to expand add a new card" 
  [on-add-card]
  (fn []
    [:button.header-button
     {:on-click on-add-card :type "button"} "Add card"]
  ))

(defn render-app
  []
  (let [filter (r/atom "")
        cards (r/cursor app-state [:cards])
        filtered (reaction (filter-by-title @filter @cards))
        handlers (event-handlers cards)
        card-renderer (render-card handlers)
        column-rendered (render-column card-renderer (handlers :on-card-drop))
        board-renderer (render-board column-rendered)]
    (fn []
      [:div
       (render-filter filter)
       [render-add-card (handlers :on-add-card)]
       [render-toggle-all (handlers :on-toggle-all)]
       [board-renderer @filtered]
      ])
    ))

(def fetch-and-render-app
  "Render the app - adding a fetching of data when the DOM is mounted"
  (with-meta render-app
    {:component-did-mount #(fake/fetch-cards! add-cards!)}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [fetch-and-render-app]
  (js/document.getElementById "app"))
