(ns clojure-rest.server
  (:require
    [clojure.java.io :as io]
    [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
    [compojure.route :refer [resources]]
    [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
    [ring.middleware.json :refer [wrap-json-response]]
    [ring.middleware.gzip :refer [wrap-gzip]]
    [ring.middleware.logger :refer [wrap-with-logger]]
    [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))


(defn get-index
  [_]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (io/input-stream (io/resource "public/index.html"))})

(defn get-cards
  [_]
  {:cards []})

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
