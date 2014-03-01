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

(defn- require-constructor-ns [sym]
  (println "quick-reset starts requiring the constructor namespaces")
  (require (symbol (namespace sym)))
  (println "quick-reset successfully required namespace of [" sym "]"))

(defn set-constructor 
  "Set the constructor function that is used to bootstrap a fresh system.

  The constructor must return 'map' with a :start and :stop field.
  Both fields contain unary (state-transition) function. They take
  the current system state as input and return a new system state.

  Example:
  A constructor in namespace 'your-namespace'
     (defn your-constructor[] 
           {:start (fn [current] (assoc current :server (new-server)))
            :stop (fn [current] (dissoc current :server)) })
  can be registered with (eg. in user.clj):
     (set-constructor 'your-namespace/your-constructor)"
  [new-constructor]
  (def loading-thread
    (on-other-thread #(require-constructor-ns new-constructor)))
  (alter-var-root #'constructor (constantly new-constructor)))

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
  (alter-var-root #'system (constantly ((resolve constructor)))))

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
