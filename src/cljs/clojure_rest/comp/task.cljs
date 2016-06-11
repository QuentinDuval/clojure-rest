(ns clojure-rest.comp.task)


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
