(ns user 
    (:use  [quick-reset.core :only  [set-constructor reset]]))

(set-constructor 'your.namespace/your-constructor)

(println "call (reset) to see the relaoding workflow in action")
