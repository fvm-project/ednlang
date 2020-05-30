(ns ednlang.core
  (:gen-class)
  (:require [fvm.core :as fvm]
            [ednlang.ednlang :as ednlang]))

(defn -main
  [filename]
  (fvm/interpret {::fvm/state {::fvm/nodes [{::fvm/type ::ednlang/requires
                                             ::ednlang/value [filename]}]}}))
