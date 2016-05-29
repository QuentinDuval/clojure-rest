(ns user
  (:require
    [clojure-rest.server]
    [ring.middleware.reload :refer [wrap-reload]]
    [figwheel-sidecar.repl-api :as figwheel]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def http-handler
  (wrap-reload #'clojure-rest.server/http-handler))

(defn start [] (figwheel/start-figwheel!))
(defn stop [] (figwheel/stop-figwheel!))

(def browser-repl figwheel/cljs-repl)


;(clojure-rest.server/start-server 8080)
;(start)
;(stop)
;(browser-repl "dev")
;:cljs/quit
