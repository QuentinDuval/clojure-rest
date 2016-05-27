(ns clojure-rest.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [clojure-rest.core-test]))

(enable-console-print!)

(doo-tests 'clojure-rest.core-test)
