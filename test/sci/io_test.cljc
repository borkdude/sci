(ns sci.io-test
  (:require
   [clojure.test :as test :refer [deftest is testing]]
   [sci.core :as sci]
   [sci.test-utils :as tu]))

(defn eval*
  ([form] (eval* nil form))
  ([binding form]
   (tu/eval* form {:bindings {'*in* binding}})))

(deftest print-test
  (when-not tu/native?
    (is (= "hello\n" (sci/with-out-str (eval* "(println \"hello\")"))))
    (is (= "hello" (sci/with-out-str (eval* "(print \"hello\")"))))
    (is (= "\"hello\"\n" (sci/with-out-str (eval* "(prn \"hello\")"))))
    (is (= "\"hello\"" (sci/with-out-str (eval* "(pr \"hello\")"))))
    (is (= "\n" (sci/with-out-str (eval* "(newline)"))))))

(deftest with-out-str-test
  (is (= "hello\n" (eval* "(with-out-str (println \"hello\"))"))))

#?(:clj
   (deftest with-in-str-test
     (is (= "hello" (eval* "(with-in-str \"hello\" (read-line))")))))

(deftest with-sci-out-str-test
  (is (= "hello\n" (sci/with-out-str (eval* "(println \"hello\")")))))
;; TODO: test for with-sci-out-str
;; TODO: test for CLJ with-sci-in-str + eval-string + read-line
