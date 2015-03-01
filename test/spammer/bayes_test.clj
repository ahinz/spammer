(ns spammer.bayes-test
  (:require [clojure.test :refer :all]
            [spammer.bayes :refer :all]))

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
  (testing "strips case"
    (let [message "thIS is THAT not This"
          [m _] (decompose-message message)]
      (is (= {"this" 2 "that" 1 "not" 1 "is" 1} m))))

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

  (testing "can build model from message strings"
    (let [strings [["Silence is golden when you can't think of a good answer." true]
                   ["It's not enough to create magic. You have to create a price for magic, too. You have to create rules." false]
                   ["The thing to remember is that that the future comes one day at a time." false]
                   ["Curious things habits. People themselves never knew they had them." true]]
          model (->> strings
                     build-model

                     (filter (fn [[k [a b]]] (or (> a 1) (> b 1))))

                     ;; Check only a subset
                     (into {}))
          model' {"a" [1 2], "that" [0 2], "have" [0 2],
                  "magic" [0 2], "the" [0 2], "to" [0 4],
                  "create" [0 3], "you" [1 2]}]
      (is (= model' model))))

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
          s2 (state-diff-for-message "to be or not to be" false)

          s1' {"to" [2 0] "be" [2 0] "or" [1 0] "not" [1 0]}
          s2' {"to" [0 2] "be" [0 2] "or" [0 1] "not" [0 1]}]
      (is (= s1' s1))
      (is (= s2' s2)))))

(deftest end-to-end
  (testing "Can build and execute model"
    (let [spams ["buy my stuff for cash"
                 "sell my stuff for cash"
                 "cash for my stuff"
                 "cash gold cash gold"]
          ok ["just got a new job"
              "what did you make for her?"]
          msgs (concat (map vector spams (repeat true))
                       (map vector ok (repeat false)))
          model (build-model msgs)]
      (is (= 0.99 (pr-message-is-spam model "cash")))
      (is (= 0.5 (pr-message-is-spam model "cash job")))
      (is (= (trunc 0.01) (trunc (pr-message-is-spam model "job")))))))
