(ns spammer.dao
  (:require [taoensso.carmine :as car]))

(defn- spam-key-for-word [w]
  (str "word:" w ":spam"))

(defn- not-spam-key-for-word [w]
  (str "word:" w ":not-spam"))

(defn clear-all
  "Clear the database"
  [db]
  (car/wcar db (car/flushall)))

(defn read-words
  "Given a sequence of words return the spam/not spam counts"
  [db words]
  (let [redis-query
        (->> words
             (mapcat (fn [k] [(spam-key-for-word k)
                              (not-spam-key-for-word k)]))
             (map car/get))
        results (car/wcar db (doall redis-query))
        [spam-count not-spam-count] (map #(Integer. %)
                                         (car/wcar db
                                                   (car/get "messagecount:spam")
                                                   (car/get "messagecount:not-spam")))
        words (->> results
                   (map #(if (nil? %) nil (Integer. %)))
                   (partition 2 2)
                   (map vector words)
                   (filter #(not
                             (or (nil? (first (second %)))
                                 (nil? (second (second %))))))
                   (into {}))]
    {:words words :total-spam spam-count :total-non-spam not-spam-count}))

(defn update-state
  "Given a redis connection and a state diff map of:
   <word> -> [<spam>, <not spam>]

   Update the backing redis store"
  [db diff num-spam-messages num-non-spam-messages]
  (let [spam-updates (map (fn [[k v]] [(spam-key-for-word k) (first v)]) diff)
        not-spam-updates (map (fn [[k v]] [(not-spam-key-for-word k) (second v)]) diff)

        keys (map first diff)

        updates (interleave spam-updates not-spam-updates)
        redis-cmds (map #(apply car/incrby %) updates)
        results (car/wcar db (doall redis-cmds))]
    (car/wcar db
              (car/incrby "messagecount:spam" num-spam-messages)
              (car/incrby "messagecount:not-spam" num-non-spam-messages))
    (->> results
         (partition 2 2)
         (map vector keys)
         (into {}))))
