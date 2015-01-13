(ns ^{:author "Alain Lehmann"
      :doc "Robustly reset application state after namespace refresh" }
  quick-reset.core
  (:require [clojure.tools.namespace.repl :refer  (refresh)]))

(def system
  "The current state of the application under development.

  System is a map with :start and :stop keys (and possible more).
  :start/:stop are 'system -> system' funcions to manipulate the system.
  See also set-constructor for more detailed information"
  nil)

(def constructor
  "The constructor for re-setting the application.
  This constructs a 'system' map with at least :start & :stop fields.
  See also set-constructor for more detailed information"
  nil)


(defn refresh-sym
  "refreshes a symbol by requiring the namespace and resolving the symbol"
  [sym]
  (-> sym namespace symbol require)
  (resolve sym))

(defn- adapt-protocol [method]
  "This adaptor allows for using protocols,
  without having an explicit dependency on them"
  (fn  [{:keys  [state] :as wrapper}]
    (assoc wrapper :state  (method state))))

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
  (alter-var-root #'constructor (constantly
       (fn [old-sys]
         ((refresh-sym new-constructor))))))

  ([create-sys start-sys stop-sys]
   (alter-var-root #'constructor (constantly
       (fn [old-sys]
           {:state ((refresh-sym create-sys))
            :start (adapt-protocol (refresh-sym start-sys))
            :stop (adapt-protocol (refresh-sym stop-sys))})))))

(defn- construct-system
  "Bootstraps a new development system and stores it in the 'system' var.
  This state is currently a singleton and behaves a bit like a state monad."
  [] 
  (alter-var-root #'system constructor))

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

(defn go
  "Creates a fresh development system and starts it running."
  [] (construct-system) (start) :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  [] (stop) (refresh :after 'quick-reset.core/go))

