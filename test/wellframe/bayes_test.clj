(ns wellframe.core-test
  (:require [clojure.test :refer :all]
            [wellframe.bayes :refer :all]))

(defn- trunc
  ([a] (trunc a 10))
  ([a i] (long (* a (Math/pow 10 i)))))

(deftest stats
  (testing "trunc"
    (is (= (trunc 0.2039244 5)
           20392)))

  (testing "calculates prob"
    (let [a-state {"word" [6 3]}]
      ;; Missing from the list? --> nil
      (is (nil? (pr-word-is-spam a-state "blah")))
      (is (= (pr-word-is-spam a-state "word") (/ 6 9)))))

  (testing "simple word list calculation"
    (let [state {"spamword0" [12 4] ;; 75%
                 "spamword1" [90 10] ;; 90%
                 "goodword0" [4 12] ;; 25%
                 "goodword1" [10 90] ;; 10%
                 }
          p (pr-word-list-is-spam state ["spamword0" "spamword1"])
          p' 0.96428571428]
      ;; p=(0.75 * 0.90)/((0.75 * 0.90) + (1.0 - 0.75)*(1.0 - 0.90))
      ;; p=0.96428571428
      (is (= (trunc p)
             (trunc p'))))))

(deftest string-functions
  (testing "handles html by stripping crap out"
    (let [message "<html><head><body color='red'>hello"
          [m v] (decompose-message message)]
      (is (= {"html" 1, "head" 1, "body" 1, "color" 1, "red" 1, "hello" 1}))))

  (testing "decompose message basic tests"
    (let [[m v] (decompose-message "to be or not to be")]
      (is (= {"to" 2 "be" 2 "or" 1 "not" 1} m))
      (is (= 6 v))))

  (testing "decomposes newlines correctly"
    (let [message "this
is
a



test
isa test"
          [m v] (decompose-message message)]
      (is (= {"is" 1 "a" 1 "isa" 1 "test" 2 "this" 1} m))
      (is (= 6 v)))))

(deftest utilities

  (testing "Can merge states"
    (let [state0 {"w0" [1 2]
                  "w1" [3 4]}
          state1 {"w1" [9 11]
                  "w2" [5 7]}
          state2 {"w1" [3 9]
                  "w3" [6 7]}
          state (additive-merge-states state0 state1 state2)
          state' {"w0" [1 2]
                  "w1" [15 24]
                  "w2" [5 7]
                  "w3" [6 7]}]
      (is (= state' state))))

  (testing "compute a model state for a given message"
    (let [s1 (state-diff-for-message "to be or not to be" true)
          s2 (state-diff-for-message "to be or not to be" false)])
    )
  )

;;
;; build-model
