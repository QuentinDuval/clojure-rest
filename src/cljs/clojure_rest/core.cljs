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

(defn filter-by-status
  "Filter card by status"
  [status cards]
  (filter #(= status (:status %)) (map second cards)))

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
     :filter "" ; BUG - If we had two boards, the cards should be the same, but not the filters
    }))

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

(defn render-card
  ; TODO - Try to remove the mess of call-backs
  "[Pure] Render a card" 
  [on-toggle-card on-remove-task on-check-task on-add-task]
  (fn [card]
    (let [card-id (:card-id card)
          details-style (when-not (:show-details card) {:style {:display "none"}})
          title-style (if (:show-details card) :div.card__title--is-open :div.card__title)]
      [:div.card
       [:div {:style (card-side-color card)}]
       [title-style {:on-click #(on-toggle-card card-id)} (:title card)]
       [:div.card__details details-style
        (:description card)
        [render-tasks (:tasks card) #(on-remove-task card-id %) #(on-check-task card-id %)]
        [render-add-task #(on-add-task card-id %)]
      ]]
  )))

(defn render-column
  [status cards card-renderer]
  [:div.list
   [:h1 (status->str status)]
   (for [c (filter-by-status status cards)]
     ^{:key (:card-id c)} [card-renderer c])
  ])

(defn render-board
  [cards card-renderer]
  [:div.app
   (for [status [:backlog :under-dev :done]]
     ^{:key status} [render-column status cards card-renderer])
  ])

(defn render-filter
  [filter-ref]
  [:input.search-input
    {:type "text" :placeholder "search"
     :value @filter-ref
     :on-change #(reset! filter-ref (.. % -target -value))}
  ])

(defn render-toggle-all
  [on-toggle-all]
  (fn []
    [:button.header-button
     {:type "button" :on-click on-toggle-all} "Expand all"]
  ))

(defn render-add-card
  [on-add-card]
  (fn []
    [:button.header-button
     {:on-click on-add-card :type "button"} "Add card"]
  ))

(defn toggle-all-cards
  ; TODO - Rework... the map is not nice
  ; It makes the rest fail (impossible to toggle afterwards)
  [cards]
  (let [all-toggled (every? #(-> % second :show-details) cards)
        toggle-card #(assoc % :show-details (not all-toggled))]
    (map (fn [[k v]] [k (toggle-card v)]) cards)
  ))

(defn render-app
  []
  ; TODO - Do not make such a big tree of functions
  ; - The DOM needs to be that deep, but not functions
  ; - But you can create the card at the top
  ; - And then you can assemble them (group-by or filter)
  (let []
    (fn []
      (let [filter (r/cursor app-state [:filter])
            cards (r/cursor app-state [:cards])
            
            on-add-card #(js/alert "toto - use route to display the form")
            on-toggle-all #(swap! cards toggle-all-cards)
            on-toggle-card #(swap! cards update-in [%1 :show-details] not)
						on-remove-task #(swap! cards update-in [%1] api/remove-task-at %2)
						on-check-task #(swap! cards update-in [%1 :tasks %2 :done] not)
						on-add-task #(swap! cards update-in [%1] api/add-task %2)
            card-renderer (render-card on-toggle-card on-remove-task on-check-task on-add-task)
            ]
        [:div
         (render-filter filter)
         [render-add-card on-add-card]
         [render-toggle-all on-toggle-all]
         [render-board (filter-by-title @filter @cards) card-renderer] ;filtered-cards
        ]))
  ))

(def fetch-and-render-app
  "Render the app - adding a fetching of data when the DOM is mounted"
  (with-meta render-app
    {:component-did-mount #(fake/fake-fetch! add-cards!)}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [fetch-and-render-app]
  (js/document.getElementById "app"))
