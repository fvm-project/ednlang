(ns profiler
  (:require [clj-async-profiler.core :as prof]
            [ednlang.core :as core]))

(defn -main []
  (prof/profile
   (core/-main "test/profile.edn"))
  (prof/serve-files 8080))
