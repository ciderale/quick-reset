(ns user 
    (:use  [quick-reset.core :only  [set-constructor reset]]))

;example using the 3-ary function
; (e.g. start/stop could be the com.stuartsierra/Lifecycle methods)
(set-constructor
  'your.component/new-component
  'your.component/start
  'your.component/stop)

;example using the 1-ary function
;(set-constructor 'your.namespace/your-constructor)


(println "call (reset) to see the relaoding workflow in action")
