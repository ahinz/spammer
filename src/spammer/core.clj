(ns spammer.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [spammer.bayes :as bayes]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(defn- slurp-dir [path n]
  (->> path
       io/file
       .listFiles
       (take n)
       (map slurp)))

(defn- create-initial-state [n]
  (let [spam-messages (map vector (slurp-dir "/Users/ahinz/Downloads/spam_2" n) (repeat true))
        ham-messages (map vector (slurp-dir "/Users/ahinz/Downloads/easy_ham" n) (repeat false))
        messages (concat spam-messages ham-messages)]
    (bayes/build-model messages)))

(def state (atom (create-initial-state 100)))

(defn- train [body is-spam]
  (swap! state bayes/additive-merge-states (bayes/build-model [[body is-spam]])))

(defn- trunc [d]
  (/ (double (int (* 100000 d))) 100000))

(defroutes app
  (GET "/model" []
       (json/write-str @state))
  (POST "/train/spam" {body :body}
        (train (slurp body) true))
  (POST "/train/not-spam" {body :body}
        (train (slurp body) false))
  (POST "/classify" {body :body}
        ;; Expected format is a json dictionary with one key:
        ;; "messages" that is an array of strings
        (let [body-content (json/read-str (slurp body) :key-fn keyword)
              messages (:messages body-content)
              state-in-time @state]
          (if (nil? messages)
            {:status 400
             :body "Expected json dictionary with a single 'messages' key"}
            (->> messages
                 (map #(trunc (bayes/pr-message-is-spam state-in-time %)))
                 (map (fn [prob] {:prob prob :is-spam (> prob 0.50)}))
                 (assoc {} :messages)
                 json/write-str))))
  (route/not-found "<h1>Page not found</h1>"))
