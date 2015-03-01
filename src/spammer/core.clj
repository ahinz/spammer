(ns spammer.core
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [spammer.bayes :as bayes]
            [spammer.dao :as dao]))

(set! *warn-on-reflection* true)

(def state (atom {}))

(defn- trunc [d]
  (/ (double (int (* 100000 d))) 100000))

(def embedded-config (io/resource "default-redis.edn"))
(def default-config-location "/etc/spammer/spammer.conf")

(defn load-config []
  (edn/read-string
   (slurp
    (if-let [env-var (System/getenv "SPAMMER_CONFIG")]
      env-var
      (if (.exists (io/as-file default-config-location))
        default-config-location
        embedded-config)))))

(def config (load-config))
(def db (:db config))
(def training-config (:data config))

(defn- train [db body is-spam]
  (dao/update-state db (bayes/build-model [[body is-spam]])))

(defn- slurp-dir [path n]
  (->> path
       io/file
       .listFiles
       (take n)
       (map slurp)))

(defn- create-training-state-from-files [n spam-path ham-path]
  (let [spam-messages (map vector (slurp-dir spam-path n) (repeat true))
        ham-messages (map vector (slurp-dir ham-path n) (repeat false))
        messages (concat spam-messages ham-messages)]
    (bayes/build-model messages)))

(defroutes app
  (POST "/train" []
        (let [training-state (create-training-state-from-files (:samples training-config)
                                  (:spam training-config)
                                  (:ham training-config))]
          (dao/update-state db training-state)))
  (POST "/flush" []
        (dao/clear-all db))
  (POST "/train/spam" {body :body}
        (train db (slurp body) true))
  (POST "/train/not-spam" {body :body}
        (train db (slurp body) false))
  (POST "/classify" {body :body}
        ;; Expected format is a json dictionary with one key:
        ;; "messages" that is an array of strings
        (let [body-content (json/read-str (slurp body) :key-fn keyword)
              messages (:messages body-content)
              state-fn #(dao/read-words db %)]
          (if (nil? messages)
            {:status 400
             :body "Expected json dictionary with a single 'messages' key"}
            (->> messages
                 (map #(trunc (bayes/pr-message-is-spam state-fn %)))
                 (map (fn [prob] {:prob prob :is-spam (> prob 0.50)}))
                 (assoc {} :messages)
                 json/write-str))))
  (route/not-found "<h1>Page not found</h1>"))
