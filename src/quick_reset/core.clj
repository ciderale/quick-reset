(ns ^{:author "Alain Lehmann"
      :doc "Robustly reset application state after namespace refresh" }
  quick-reset.core
  (:require [clojure.tools.namespace.repl :refer  (refresh)]))

(def constructor 
  "The constructor for re-setting the application."
  nil)

(def system 
  "The current state of the application under development."
  nil)

(defn- on-other-thread 
  "The REPL start fails when a required namespace has compilation errors.
  This problem can be handled by requiring namespaces in another thread.
  For some reasons, this works with plain thread, but not with futures."
  [func]
  (doto (Thread. (reify Runnable (run [_] (func))))
    .start))

(defn- require-constructor-ns [& syms]
  (println "quick-reset starts requiring the constructor namespaces")
  (->> syms (map namespace) set (map symbol) (map require))
  (println "quick-reset successfully required namespace of [" syms "]"))

(defn set-constructor 
  "Set the constructor function that is used to bootstrap a fresh system.

  The constructor must return 'map' with a :start and :stop field.
  Both fields contain unary (state-transition) function. They take
  the current system state as input and return a new system state.

  The 3ary version create the above mentioned map from three functions.
   - new-sys : () -> system
   - start-sys : system -> 'started' system 
   - stop-sys  : system -> 'stopped' system

  Example for the 3ary function:
  This particularly helpful when working with a component library like
  e.g. com.stuartsierra/component as c ; then the required call is:
     (set-constructor 'your-ns/create-system-map 'c/start 'c/stop)
  with (defn create-system-map [] (c/system-map ... )

  Example for the 1ary call:
  A constructor in namespace 'your-namespace'
     (defn your-constructor[] 
           {:start (fn [current] (assoc current :server (new-server)))
            :stop (fn [current] (dissoc current :server)) })
  can be registered with (eg. in user.clj):
     (set-constructor 'your-namespace/your-constructor)"
  ([new-constructor]
  (def loading-thread
    (on-other-thread #(require-constructor-ns new-constructor)))
  (alter-var-root #'constructor (constantly #((resolve new-constructor)))))

  ([new-sys start-sys stop-sys]
   (def loading-thread
     (on-other-thread 
       #(require-constructor-ns new-sys start-sys stop-sys)))
   (letfn [(act  [action]
             (fn  [{:keys  [system] :as wrapper}]
               (assoc wrapper :system  ((resolve action) system))))]
     (alter-var-root #'constructor 
       (constantly (fn [_] {:system ((resolve  new-sys)) 
                            :start (act start-sys)
                            :stop (act stop-sys)}))))))

(defn- avoid-race-condition-with-constructor-requiring-thread 
  "The thread should only be joined *after* the REPL has fully started.
  This prevents startup failures when requiring erroneous namespaces.
  Therefore, this call is postponed to the construct-system call rather
  than doing it in the set-constructor call."
  [] (.join loading-thread))

(defn- construct-system
  "Bootstraps a new development system and stores it in the 'system' var.
  This state is currently a singleton and behaves a bit like a state monad."
  [] 
  (avoid-race-condition-with-constructor-requiring-thread)
  (alter-var-root #'system constructor))
  ;(alter-var-root #'system (constantly (constructor))))

(defn- monadic-call [key]
  (when system
    (when-let [method (key system)]
      (alter-var-root #'system method))))

(defn start
  "Starts the current development system."
  [] (monadic-call :start) :started)

(defn stop
  "Shuts down the current development system."
  [] (monadic-call :stop) :stopped)

(defn- go
  "Creates a fresh development system and starts it running."
  [] (construct-system) (start) :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  [] (stop) (refresh :after 'quick-reset.core/go))

