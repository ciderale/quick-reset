(defproject ciderale/quick-reset "0.5.0"
  :description "Quick Application Reset after Namespace Refresh
               for Rapid Clojure Development Cycles"
  :url "http://github.com/ciderale/quick-reset"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.namespace "0.2.4"]]

  ;the following profile is only used to demonstrate the "example"
  :profiles {:dev {:source-paths ["example"]}})
