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
  "Filter card by status"
  [status cards]
  (filter #(= status (-> % deref :status)) cards))

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
  ; BUG - Keeping the ::show-details at app-state means that rendering twice
  ; the component would lead to the GUI being updated at two places
  ; => It cannot be in the component alone (if you want the expand all)
  ;    But it should be in a state specific to the board (assoc container)
  (let [default-state {::show-details false}
        to-gui-card #(vector (:card-id %) (merge % default-state))]
    (swap! app-state
     #(update-in % [:cards] merge (map to-gui-card cards))
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
  [card-ref]
  (let [on-remove #(swap! card-ref api/remove-task-at %)
        on-check #(swap! card-ref update-in [:tasks % :done] not)]
    [:div.checklist
     [:ul
      (map
        (fn [idx t]
          ^{:key t} [render-task #(on-remove idx) #(on-check idx) t])
        (range)
        (:tasks @card-ref)
      )]
     ]))

(defn render-add-task
  "Render the text field allowing to add new tasks to a card"
  [card-ref]
  [:input.checklist--add-task
   {:type "text"
    :placeholder "Type then hit Enter to add a task"
    :on-key-press
    (fn [e]
      (when (= "Enter" (.-key e))
        (swap! card-ref api/add-task (.. e -target -value))
        (set! (.. e -target -value) "")
      ))
   }])

(defn render-card
  [card-ref]
  (let [show-details (::show-details @card-ref)
        toggle-details #(swap! card-ref update-in [::show-details] not)
        title-style (if show-details :div.card__title--is-open :div.card__title)
        details-style (when-not show-details {:style {:display "none"}})]
    [:div.card
     [:div {:style (card-side-color @card-ref)}]
     [title-style {:on-click toggle-details} (:title @card-ref)]
     [:div.card__details details-style
      (:description @card-ref)
      [render-tasks card-ref]
      [render-add-task card-ref]
    ]]
  ))

(defn render-column
  [status card-refs]
  [:div.list
   [:h1 (status->str status)]
   (for [c (filter-by-status status card-refs)]
     ^{:key (:card-id @c)} [render-card c])
  ])

(defn render-board
  [cards]
  [:div.app
   (for [status [:backlog :under-dev :done]]
     ^{:key status} [render-column status cards])
  ])

(defn render-filter
  [filter-ref]
  [:input.search-input
    {:type "text" :placeholder "search"
     :value @filter-ref
     :on-change #(reset! filter-ref (.. % -target -value))}
  ])

(defn render-toggle-all
  [card-refs]
  ; TODO - Does not work so good: you need the state of other cards
  ; Because they might not be in the same state => the label must change
  (let [toggle #(update-in % [::show-details] not)
        on-click (fn []
                   (doall
                     (map #(swap! % update-in [::show-details] not) card-refs))
                 )]
    ; !!! The following handler works when given @cards
    ; But no refresh occurs! And the screen is blocked
    ; Even if the cursors are re-created...
    ; on-click2 (fn [] (swap! card-refs #(map toggle %)))
    ; 
    ; Cursors are hard to make work:
    ; => Use props and messages, but then you must provide call-backs
    ; => But the worst is that you have to give the path accross functions
    ; => INSTEAD try to create the component high (easy callback)
    ; => And then compose them into views
    [:button.header-button
     {:on-click on-click :type "button"} "Expand all"]
  ))

(defn render-add-card
  [card-refs]
  (let [on-click #(js/alert "toto - use route to display the form")]
    [:button.header-button
     {:on-click on-click :type "button"} "Add card"]
  ))

(defn render-app
  []
  ; TODO - Do not make such a big tree of functions
  ; - The DOM needs to be that deep, but not functions
  ; - But you can create the card at the top
  ; - And then you can assemble them (group-by or filter)
  (let [filter (r/cursor app-state [:filter])
        cards (r/cursor app-state [:cards])
        filtered-cards (reaction (filter-by-title @filter @cards))
        card-refs (map #(r/cursor app-state [:cards (first %)]) @filtered-cards)]
    [:div
     ; TODO - Try to add a button to show details to all tickets
     (render-filter filter)
     [render-add-card cards]
     [render-toggle-all card-refs]
     [render-board card-refs]]
  ))

(def fetch-and-render-app
  "Render the app - adding a fetching of data when the DOM is mounted"
  (with-meta render-app
    {:component-did-mount #(fake/fake-fetch! add-cards!)}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(r/render [fetch-and-render-app]
  (js/document.getElementById "app"))
