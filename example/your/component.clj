(ns your.component
  "example component to demonstrate the reloading workflow")

(defn new-component [] "initial-component")

(defn start [component]
  (println "..starting.. was " component)
  "started-component")

(defn stop [component]
  (println "..stopping.. was " component)
  "stopped-component")

;uncomment and let (quick-reset.core/reset) deals with compilation errors
;adding-some-unknown-symbol

