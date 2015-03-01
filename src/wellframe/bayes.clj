(ns wellframe.bayes
  (:require [clojure.string :as s]))

;; http://en.wikipedia.org/wiki/Naive_Bayes_spam_filtering
(set! *warn-on-reflection* true)

(def max-pr 0.99)
(def min-pr 0.01)

(defn pr-word-is-spam
  "Given the current execution state, determine the probability
   that the input word is spam

   Note that this probability is used in a log formulation such as:
   ln(P) and ln(1-P)

   Therefore the values returned by this method are clamped to
   max-pr and min-pr
  "
  ([state word] (pr-word-is-spam state word max-pr min-pr))
  ([state word max-pr min-pr]
   (let [[spam-count not-spam-count] (get state word)]
     (if (or (nil? spam-count)
             (nil? not-spam-count))
       nil
       (-> (/ spam-count (+ spam-count not-spam-count))
           (min max-pr)
           (max min-pr))))))

(defn- ln [x]
  (Math/log x))

(defn- log [x]
  (println x) x)

(defn pr-word-list-is-spam
  "Given list of words compute the combined probability that
   the word list is spam using:
   p = (p_1 * p_2 * ... * p_n) /
       ((p_1 * p_2 * ... * p_n) + (1 - p_1)(1 - p_2) * ... * (1 - p_N))"
  [state word-list]
  ;; To avoid floating point error we can use a log formulation instead
  (let [η (->> word-list
               (map #(pr-word-is-spam state %))
               (filter #(not (nil? %)))
               (map #(- (ln (- 1.0 %)) (ln %)))
               (reduce +))]
    (/ 1.0 (+ 1.0 (Math/exp η)))))

(defn- replace-all [^String t s v]
  (.replaceAll t s v))

(defn decompose-message
  "Decompose a string/html message into a tuple of
   [map from <word> -> <count>, # of total words"
  [^String message]
  (let [words (-> message
                  .toLowerCase
                  ;; Remove non text characters
                  (replace-all "[^A-Za-z \n\r]" " ")

                  ;; Split via any whitespace
                  (s/split #"[ \n\r]+"))
        ;; Exclude the empty string
        words (filter #(> (count %) 0) words)
        total-words (count words)
        words-by-count (->> words
                            (group-by identity)
                            (map (fn [[k v]] [k (count v)]))
                            (into {}))]
    [words-by-count total-words]))

(defn state-diff-for-message
  "The state map is a map from <String> -> (# spam, # not spam)

  This function computes a new state map for the given message"
  [message is-spam]
  (let [[values _] (decompose-message message)
        create-diff (fn [[k v]] [k (if is-spam [v 0] [0 v])])]
    (->> values
         (map create-diff)
         (into {}))))

(defn additive-merge-states
  "Merge the given state

  If a word is in more than one state the values will be added together"
  [& states]
  (apply merge-with (fn [& values]
                      (let [spam-count (reduce + (map first values))
                            not-spam-count (reduce + (map second values))]
                        [spam-count not-spam-count]))
         states))

(defn build-model
  "Given a sequence of tuples like (message, is-spam)
  generate a state for the model"
  [messages]
  (->> messages
       (map #(apply state-diff-for-message %))
       (reduce additive-merge-states)))

(defn pr-message-is-spam
  "Give the probability that the input message is spam"
  [state message]
  (->> message
       decompose-message

       ;; Get the word list
       first

       ;; Strip the counts
       (map first)

       (pr-word-list-is-spam state)))
