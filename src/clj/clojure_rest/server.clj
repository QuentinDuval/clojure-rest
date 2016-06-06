(ns clojure-rest.server
  (:require
    [clojure.java.io :as io]
    [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
    [compojure.route :refer [resources]]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
    [ring.middleware.json :refer [wrap-json-response]]
    [ring.middleware.gzip :refer [wrap-gzip]]
    [ring.middleware.logger :refer [wrap-with-logger]]
    [ring.util.response :as resp]
    [clojure-rest.card :as card])
  (:gen-class))


(defn get-index
  [_]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "public/index.html"))})

(defn get-cards
  [_] ; TODO - Replace with correct implementation
  (resp/response
    {:cards
     [(card/create-card!
        "My card" "My card description" card/BUG-FIX card/DONE
        (card/create-task "1st task" true)
        (card/create-task "2nd task" false))
     ]}
  ))

(defroutes routes
  (GET "/" _ get-index)
  (GET "/cards" _ (wrap-json-response get-cards)) 
  (resources "/"))

(def http-handler
  (-> routes (wrap-defaults api-defaults) wrap-with-logger wrap-gzip))

(defn start-server
  [port]
  (run-jetty http-handler {:port port :join? false}))

(defn -main [& [port]]
  (let [port (Integer. (or port 8080))]
    (start-server port)))
