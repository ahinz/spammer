(ns spammer.core-test
  (:import [java.io ByteArrayInputStream])
  (:require [ring.mock.request :refer :all]
            [clojure.test :refer :all]
            [clojure.data.json :as json]
            [spammer.bayes :as bayes]
            [spammer.core :refer :all]))

(defn wrap-as-stream [s]
  (ByteArrayInputStream. (.getBytes s "UTF-8")))

(defn post-request [resource body]
  (let [req (request :post (str "https://example.com:8443" resource) {})
        req-with-body (assoc req :body (wrap-as-stream body))]
    (app req-with-body)))

(deftest training-route
  (testing "can train spam"
    (reset! state {})
    (let [msg1 "this is a test"
          msg2 "what could this be? The test!"]

      (is (= 200 (:status (post-request "/train/spam" msg1))))
      (is (= 200 (:status (post-request "/train/not-spam" msg2))))

      (is (= @state (bayes/build-model [[msg1 true]
                                        [msg2 false]]))))))

(deftest classify
  (testing "can classify spam"
    (reset! state {})
    (let [msg1 "this is spam"
          msg2 "this is wonderful"

          test-msgs {:messages ["wonderful" "spam"]}
          test-msgs-str (json/write-str test-msgs)]

      (is (= 200 (:status (post-request "/train/spam" msg1))))
      (is (= 200 (:status (post-request "/train/not-spam" msg2))))

      (let [result (post-request "/classify" test-msgs-str)
            resultb (json/read-str (:body result) :key-fn keyword)

            messages (:messages resultb)]
        (is (= (map :is-spam messages) [false true]))))))
