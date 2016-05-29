(ns user
  (:require
    [clojure-rest.server]
    [ring.middleware.reload :refer [wrap-reload]]
    [figwheel-sidecar.repl-api :as figwheel]))

;(set! *warn-on-reflection* true)
;(set! *unchecked-math* :warn-on-boxed)
;(enable-console-print!)

(def http-handler
  (wrap-reload #'clojure-rest.server/http-handler))

(defn start [] (figwheel/start-figwheel!))
(defn stop [] (figwheel/stop-figwheel!))

(def browser-repl figwheel/cljs-repl)

;(clojure-rest.server/start-server 8080)
;(start)
;(browser-repl "dev")
;:cljs/quit
;(stop)
