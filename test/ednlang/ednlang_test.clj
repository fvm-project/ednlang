(ns ednlang.ednlang-test
  (:require [clojure.test :refer :all]
            [fvm.core :as fvm]
            [fvm.util :as u]
            [ednlang.ednlang :as ednlang]))

;; Utils
;; =====
(defn run [state]
  (::fvm/state (fvm/interpret {::fvm/state state})))

(defn apply-op [op stack-in]
  (::ednlang/stack (run {::fvm/nodes [{::fvm/type op}]
                         ::ednlang/stack stack-in})))

(defmacro timing [expr]
  `(let [start-ms# (u/curr-millis)
         _# ~expr
         end-ms# (u/curr-millis)]
     (- end-ms# start-ms#)))


;; Tests
;; =====
(deftest math-test
  (let [stack-in [2 3]]
    (is (= [5] (apply-op ::ednlang/add stack-in)))
    (is (= [-1] (apply-op ::ednlang/sub stack-in)))
    (is (= [6] (apply-op ::ednlang/mul stack-in)))
    (is (= [2/3] (apply-op ::ednlang/div stack-in)))))

(deftest logic-test
  (let [eq-insn {::fvm/type ::ednlang/eq?
                 ::ednlang/then [{::fvm/type ::ednlang/pop}
                                 {::fvm/type ::ednlang/pop}
                                 {::fvm/type ::ednlang/push
                                  ::ednlang/value true}]
                 ::ednlang/else [{::fvm/type ::ednlang/pop}
                                 {::fvm/type ::ednlang/pop}
                                 {::fvm/type ::ednlang/push
                                  ::ednlang/value false}]}]
    (is (= {::fvm/nodes []
            ::ednlang/stack [true]}
           (run {::fvm/nodes [eq-insn]
                 ::ednlang/stack [:a :a]})))
    (is (= {::fvm/nodes []
            ::ednlang/stack [false]}
           (run {::fvm/nodes [eq-insn]
                 ::ednlang/stack [:a :b]})))))

(deftest stack-test
  (is (= [:a 1 2]
         (::ednlang/stack (run {::fvm/nodes [{::fvm/type ::ednlang/push
                                              ::ednlang/value :a}]
                                ::ednlang/stack [1 2]}))))
  (is (= [2] (apply-op ::ednlang/pop [1 2])))
  (is (= [:a :a] (apply-op ::ednlang/dup [:a])))
  (is (= [2 1] (apply-op ::ednlang/swap [1 2]))))

(deftest macros-test
  (is (= {::fvm/nodes []
          ::ednlang/stack [1 1]}
         (run {::fvm/nodes [{::fvm/type ::ednlang/push
                             ::ednlang/value [{::fvm/type ::ednlang/push
                                               ::ednlang/value 1}
                                              {::fvm/type ::ednlang/dup}]}
                            {::fvm/type ::ednlang/call}]})))
  (is (= {::fvm/nodes []
          ::ednlang/stack [7]}
         (run {::fvm/nodes [{::fvm/type ::ednlang/defop
                             ::ednlang/name ::ednlang/add-2
                             ::ednlang/value [{::fvm/type ::ednlang/push
                                               ::ednlang/value 2}
                                              {::fvm/type ::ednlang/add}]}

                            {::fvm/type ::ednlang/push
                             ::ednlang/value 5}
                            {::fvm/type ::ednlang/add-2}]}))))

(deftest io-test
  (is (= [3]
         (::ednlang/stack
          (with-in-str "1 2"
            (run {::fvm/nodes [{::fvm/type ::ednlang/read}
                               {::fvm/type ::ednlang/read}
                               {::fvm/type ::ednlang/add}]})))))
  (is (= "hi"
         (with-out-str
           (apply-op ::ednlang/print ["hi"])))))

(deftest requires-test
  (is (= {::fvm/nodes []
          ::ednlang/stack [2]}
         (run {::fvm/nodes [{::fvm/type ::ednlang/requires
                             ::ednlang/value ["test/test.edn"]}
                            {::fvm/type ::ednlang/push
                             ::ednlang/value 1}
                            {::fvm/type :test/inc}]}))))

(deftest vm-test
  (testing "self tail recursive ops are jitted"
    (let [range-script [{::fvm/type ::ednlang/defop
                         ::ednlang/name :test/range
                         ::ednlang/value [{::fvm/type ::ednlang/push
                                           ::ednlang/value 0}
                                          {::fvm/type ::ednlang/eq?
                                           ::ednlang/then [{::fvm/type ::ednlang/pop}
                                                           {::fvm/type ::ednlang/pop}]
                                           ::ednlang/else [{::fvm/type ::ednlang/pop}
                                                           {::fvm/type ::ednlang/dup}
                                                           {::fvm/type ::ednlang/push
                                                            ::ednlang/value 1}
                                                           {::fvm/type ::ednlang/swap}
                                                           {::fvm/type ::ednlang/sub}
                                                           {::fvm/type :test/range}]}]}]

          run-interpreted
          #(fvm/interpret
            {::fvm/state {::fvm/nodes (concat range-script
                                              [{::fvm/type ::ednlang/push
                                                ::ednlang/value %}
                                               {::fvm/type :test/range}])}})

          test-interpreted-op #(-> (run-interpreted %)
                                   ::fvm/state
                                   ::ednlang/stack)
          final-state (run-interpreted 100)
          compiled-op (-> final-state
                          ::fvm/trace-info
                          :test/range
                          ::fvm/compiled-node)
          test-compiled-op (fn [n]
                             (-> {::ednlang/stack [n]}
                                 compiled-op
                                 ::ednlang/stack))]
      (testing "meta"
        (is (true? (-> @fvm/node-opts :test/range ::fvm/jit?)))

        (is (fn? (-> final-state ::fvm/trace-info :test/range ::fvm/compiled-node))))

      (testing "correctness"
        (is (= (range 1 6)
               (test-interpreted-op 5)))

        (is (= (range 1 6)
               (test-compiled-op 5)))

        (is (= (range 1 201)
               (test-compiled-op 200))))

      (testing "performance"
        (is (< (timing (test-compiled-op 300))
               (timing (test-interpreted-op 300)))))))

  (testing "non-tail recursive ops are excluded from jit"
    (let [fact-script [{::fvm/type ::ednlang/requires
                        ::ednlang/value ["lib/std.edn"]}

                       {::fvm/type ::ednlang/defop
                        ::ednlang/name :test/fact
                        ::ednlang/value [{::fvm/type ::ednlang/push
                                          ::ednlang/value 0}
                                         {::fvm/type ::ednlang/eq?
                                          ::ednlang/then [{::fvm/type ::ednlang/pop}
                                                          {::fvm/type ::ednlang/pop}
                                                          {::fvm/type ::ednlang/push
                                                           ::ednlang/value 1}]
                                          ::ednlang/else [{::fvm/type ::ednlang/pop}
                                                          {::fvm/type ::ednlang/dup}
                                                          {::fvm/type ::ednlang/dec}
                                                          {::fvm/type :test/fact}
                                                          {::fvm/type ::ednlang/mul}]}]}]
          run-fact #(run {::fvm/nodes (concat fact-script
                                              [{::fvm/type ::ednlang/push
                                                ::ednlang/value %}
                                               {::fvm/type :test/fact}])})
          final-state (run-fact 100)]
      (testing "meta"
        (is (false? (-> @fvm/node-opts :test/fact ::fvm/jit?)))

        (is (nil? (-> final-state ::fvm/trace-info :test/fact ::fvm/compiled-node))))

      (testing "correctness"
        (is (= (apply *' (range 1 6))
               (-> (run-fact 5)
                   ::ednlang/stack
                   first)))

        (is (= (apply *' (range 1 201))
               (-> (run-fact 200)
                   ::ednlang/stack
                   first)))))))
