(ns clojure-rest.comp.search)


(defn render-filter
  "[Side effect] Render the filter to only show cards containing a given text" 
  [filter-ref]
  [:input.search-input
    {:type "text" :placeholder "search"
     :value @filter-ref
     :on-change #(reset! filter-ref (.. % -target -value))}
  ])
