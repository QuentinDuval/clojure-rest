(ns clojure-rest.comp.card
  (:require
    [clojure-rest.card :refer [BUG-FIX ENHANCEMENT]]
    [clojure-rest.utils :as utils]
    [clojure-rest.comp.task :as task]
  ))

(defn- category->color
  [category]
  (condp = category
    BUG-FIX "#BD8D31"
    ENHANCEMENT "#3A7E28"))

(defn- card-side-color
  "Render the ribbon on the left of the card, that indicates its category"
  [card]
  {:position "absolute" :zindex -1 :top 0 :bottom 0 :left 0 :width 5
   :backgroundColor (-> card :category category->color)
  })

(defn- render-card-details
  "[Pure] Render the details of a card (description and tasks)"
  [dispatch {:keys [card-id description show-details tasks]}]
  [:div.card__details
   (when-not show-details {:style {:display "none"}})
   description
   [task/render-tasks tasks #(dispatch :on-remove-task card-id %)
                            #(dispatch :on-check-task card-id %)]
   [task/render-add-task #(dispatch :on-add-task card-id %)]
  ])

(defn render-card
  "[Pure] Render a card"
  [dispatch]
  (fn [{:keys [card-id title show-details] :as card}]
    (let [title-style (if show-details :div.card__title--is-open :div.card__title)]
      [:div.card
       {:draggable true
        :onDragStart #(utils/set-transfer-data % :card-id card-id)}
       [:div {:style (card-side-color card)}]
       [title-style {:on-click #(dispatch :on-toggle-card card-id)} title]
       [render-card-details dispatch card] 
      ])
    ))