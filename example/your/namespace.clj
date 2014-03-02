(ns your.namespace
  "example namespace to demonstrate the reloading workflow")

(defn start [system]
  (println "..starting.. was " (:state system))
  (assoc system :state :started))

(defn stop [system]
  (println "..stopping.. was " (:state system))
  (assoc system :state :stopped))

(defn your-constructor  [] 
  (println "..constructing..")
  {:state :initialized, :start  start, :stop  stop})

;uncomment and let (quick-reset.core/reset) deals with compilation errors
;adding-some-unknown-symbol

