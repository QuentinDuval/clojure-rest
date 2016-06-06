(ns user
  (:require
    [clojure-rest.server]
    [ring.middleware.reload :refer [wrap-reload]]
    [figwheel-sidecar.repl-api :as figwheel]
    [ring.adapter.jetty :refer [run-jetty]]
  ))

;(set! *warn-on-reflection* true)
;(set! *unchecked-math* :warn-on-boxed)
;(enable-console-print!)

(def http-handler
  (wrap-reload #'clojure-rest.server/http-handler))

(defn start-server [port]
  (run-jetty http-handler {:port port :join? false}))

(defn start [] (figwheel/start-figwheel!))
(defn stop [] (figwheel/stop-figwheel!))
(def browser-repl figwheel/cljs-repl)

;(start)
;(start-server 8080)
;(browser-repl "dev")
;:cljs/quit
;(stop)
