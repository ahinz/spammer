(ns wellframe.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [wellframe.bayes :as bayes]
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

(defroutes app
  (GET "/model" []
       (println (count @state))
       (str @state))
  (POST "/train/spam" {body :body}
        (train (slurp body) true))
  (POST "/train/not-spam" {body :body}
        (train (slurp body) false))
  (POST "/classify" {body :body}
        (str (bayes/pr-message-is-spam @state (slurp body))))
  (route/not-found "<h1>Page not found</h1>"))
