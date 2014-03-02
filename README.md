# quick-reset

This clojure library embraces the ideas  of
[Stuart Sierra's clojure development workflow, reloaded][blog].

This library bundles the re-occurring workflow logic and tries to 
gracefully handle compilation errors in required namespaces. 
In particular, the `(reset)` functionality is not lost
upon failed refreshes. Moreover, the REPL also starts despite compilation
errors in referenced namespaces. Both problems were described in the
original post and required an annoying restart of the REPL.
This library is robust against these errors and leaves the REPL in 
a sane state.

[Please refer to the original blog post for a detailed description of
the workflow][blog].    
A quick summary and the necessary configuration
steps are described below.

## Usage at the REPL

This library allows for rapid development cycles with up-to-date
sources.    
A single command is enough to refresh all modified namespaces
and to creates a brand new, started application.   

```clojure
user=> (reset)   ; whenever you want a fresh system
```
This stops your current application, refreshes all modified namespaces,
creates a fresh instance of your application, and starts it.

## Setup


The latest version is 
```clojure
[ciderale/quick-reset "0.1.1"]
```

#### Add dependency in your Leiningen `project.clj`

```clojure
{ ;; your project configuration
 :profiles {:dev {:source-paths ["dev"] ; or wherever you put your `user.clj`
                  :dependencies  [[ciderale/quick-reset "0.1.1"]]}}}
```
This assumes that your `user.clj` resides in the `dev/` folder within 
your project.

#### Prepare your REPL with code in your `user.clj`
```clojure
(ns user 
  (:use [quick-reset.core :only [set-constructor reset]]))
 
  (set-constructor 'your.namespace/your-constructor)
```


#### Create an application constructor in `your/namespace.clj` of choice

This library requires a constructor function to initialize a fresh
application.    
Note that this function is called for each invocation of
(reset), after successfully reloading the namespaces.

The following code illustrates a simple application scenario:

```clojure
(ns your.namespace)

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
```

The constructor must returns a map representing the initial
application configuration.  `quick-reset` requires this map to contain
:start and :stop fields.  These fields keep functions that take the
current state, manipulate that state, and return the new state. 

This configuration is essentially the only global variable in your
system. All your sub systems should (indirectly) refer to this
configuration. Note that using other global variables (e.g. via `def`)
will potentially lead to an incomplete system refresh. You have been
warned..


## The example in action

The following is the expected output with the above setup

```clojure
lein repl
; quite some output from the REPL startup may interfere/interleave
user=> (reset)
:reloading (your.namespace)
..initializing..
..starting.. was :initialized
:ready
user=> (reset)
..stopping.. was :started
:reloading ()
..initializing..
..starting.. was :initialized
:ready
user=> (quick-reset.core/stop)
..stopping.. was :started
:stopped
user=> (quick-reset.core/start)  ; just start, no reload/re-initialize
..starting.. was :stopped
:started
```

Adding an `adding-some-unknown-symbol` in `your/namespace.clj` lets
the refresh fail, but the REPL is still functional, the system is
:stopped, and (reset) can be called after the error is fixed in the
source file.

```clojure
user=> (reset)
..stopping.. was  :started
:reloading (your.namespace)
:error-while-loading your.namespace
#<CompilerException java.lang.RuntimeException: Unable to resolve symbol: adding-some-unknown-symbol in this context, compiling:(your/namespace.clj:1:1)>
user=> quick-reset.core/system
{:state :stopped, 
 :start #<namespace$start your.namespace$start@534041ec>, 
 :stop #<namespace$stop your.namespace$stop@54a8e52d>}
user=> (reset)
..stopping.. was  :stopped
:reloading (your.namespace)
..constructing..
..starting.. was  :initialized
:ready
```

## Technical Notes

Inversion of control (IoC) allows that this library has no
dependencies on a concrete system.  The only reference to concrete
systems is injected via the `constructor` as detailed above.
Consequently, changes in your source files and a subsequent namespace
refresh do not affect the `quick-reset` namespace.  This is why
`(reset)` will remain accessible even if the refresh of certain
namespaces fails. This makes the refreshes robust against compilation
errors.

The second problem is that erroneous namespaces hindered the REPL from
starting. This is addressed by loading the constructor function's
namespace (and all its dependencies) in a separate thread. A failure
thus no longer jeopardizes the startup process of the REPL.

## Credits

Thanks to Stuart Sierra for the description of the 
[namespace reloading clojure workflow][blog].

## License

Copyright © 2014 Alain Lehmann

Distributed under the Eclipse Public License, the same as Clojure.



[blog]: <http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded>
